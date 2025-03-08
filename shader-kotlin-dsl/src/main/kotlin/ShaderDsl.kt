package io.github.etieskrill.extension.shader.dsl

import io.github.etieskrill.extension.shader.dsl.ShaderBuilder.MethodProgramStatement
import io.github.etieskrill.extension.shader.dsl.inner.TestShader
import io.github.etieskrill.extension.shader.dsl.inner.Vertex
import io.github.etieskrill.injection.extension.shader.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.intellij.lang.annotations.Language
import org.joml.Vector4f
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

@DslMarker
@Target(AnnotationTarget.CLASS)
internal annotation class ShaderDslMarker

/**
 * @param VA the vertex attributes
 * @param V the internal vertex type
 * @param RT the render targets
 */
@ShaderDslMarker
abstract class ShaderBuilder<VA : Any, V : Any, RT : Any>(
    private val vertexAttributesClass: KClass<VA>,
    private val vertexClass: KClass<V>,
    private val renderTargetsClass: KClass<RT>,
) {

    var vertexData: VA? = null

    private val vertexAttributes: VA = proxyObject(vertexAttributesClass, "data")
    private val vertex: V = proxyObject(vertexClass, "data")

    private val vertexAttributeStatements = mutableMapOf<String, KClass<*>>()
    private val vertexDataStatements = mutableMapOf<String, KClass<*>>()
    private val renderTargetStatements = mutableMapOf<String, KClass<*>>()

    internal var callDepth = -1

    private val uniformStatements = mutableMapOf<Any, UniformStatement>()

    internal val programStatements = mutableListOf<ProgramStatement>()

    internal data class UniformStatement(
        val type: KClass<*>,
        val proxy: Any,
        var name: String? = null
    ) {
        val used: Boolean
            get() = name != null
    }

    internal open class ProgramStatement(open val callDepth: Int)

    private data class UniformProgramStatement(
        val uniform: UniformStatement,
        override val callDepth: Int,
    ) : ProgramStatement(callDepth)

    internal data class MethodProgramStatement(
        val returnType: KClass<*>,
        override val callDepth: Int,
        val name: String,
        val args: List<*>
    ) : ProgramStatement(callDepth)

    private data class ReturnValueProgramStatement(
        val value: Any,
        override val callDepth: Int
    ) : ProgramStatement(callDepth)

    init {
        vertexAttributesClass.check()
        vertexClass.check()
        renderTargetsClass.checkOpen()

        vertexAttributeStatements.putAll(vertexAttributesClass.getFields("vertex attributes"))
        vertexDataStatements.putAll(vertexClass.getFields("vertex data fields"))
        renderTargetStatements.putAll(renderTargetsClass.getFields("render targets"))
    }

    private fun KClass<*>.check() {
        checkOpen()
        require(constructors.any {
            it.parameters.isEmpty() || it.parameters.all { it.isOptional }
        }) { "${this.simpleName} constructor must have a no-arg constructor" }
    }

    private fun KClass<*>.checkOpen() =
        require(isOpen) { "${this.simpleName} class must be open" }

    private fun KClass<*>.getFields(type: String): Map<String, KClass<*>> = members
        .filterNot { it.name in listOf("equals", "hashCode", "toString") }
        .onEach { field ->
            require(field !is KMutableProperty)
            { "All $type should be immutable: ${simpleName}.${field.name} is mutable" }
            require(field.isOpen)
            { "All $type must be open: ${simpleName}.${field.name} is not open" }
        }
        .associate { it.name to it.returnType.classifier as KClass<*> }

    protected inner class UniformDelegate<T : Any>(clazz: KClass<T>) : ReadWriteProperty<ShaderBuilder<VA, V, RT>, T> {
        private var value: T = proxyObject(clazz, "uniform")

        init {
            this@ShaderBuilder.uniformStatements[value.id] = UniformStatement(
                clazz,
                value
            ) //using the system object id, because hashCode logically returns identical values for identical matrices
        }

        override fun getValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>): T {
            //TODO use flag to *actually* switch behaviour to get the value from shader (or cached) when outside of program definition
            val uniformStatement = this@ShaderBuilder.uniformStatements[value.id]!!
            if (!uniformStatement.used) uniformStatement.name = property.name
            this@ShaderBuilder.programStatements.add(UniformProgramStatement(uniformStatement, callDepth))
            return value
        }

        override fun setValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>, value: T) {
            //TODO use flag to throw on setting uniform property in program definition
            //property is only writeable to allow uniform to actually be set from outside
            this.value = value
        }
    }

    protected inline fun <reified T : Any> uniform() = UniformDelegate(T::class)

    protected fun vertex(block: VertexReceiver.(VA) -> V) {
        val vertex = block(VertexReceiver(this), vertexAttributes)
        programStatements.add(ReturnValueProgramStatement(vertex, callDepth))
    }

    protected fun fragment(block: FragmentReceiver.(V) -> RT) {
        val renderTargets = block(FragmentReceiver(this), vertex)
        programStatements.add(ReturnValueProgramStatement(renderTargets, callDepth))
    }

//    init {
//        program()
//        check(callDepth == -1) { "Not all proxied function calls returned. This... should not happen." }
//    }

    protected abstract fun program()

    internal fun generateGlsl() {
        println(programStatements.joinToString("\n"))

        val source = buildString {
            glslVersion()
            newline()
            glslStruct(vertexClass, vertexDataStatements)
            newline()
            glslUniforms(uniformStatements.values.toList())
            newline()

            glslStage(ShaderStage.VERTEX)
            newline()
            glslVertexAttributes(vertexAttributeStatements)
            newline()
            glslStatement(GlslStorageQualifier.OUT, vertexClass, "vertex")
            newline()
            glslMain()
            newline()

            glslStage(ShaderStage.FRAGMENT)
            newline()
            glslStatement(GlslStorageQualifier.IN, vertexClass, "vertex")
            newline()
            glslRenderTargets(renderTargetStatements)
            newline()
            glslMain()
        }

        File("Test.glsl").writeText(source)
    }

    //TODO map identityHashCode to instance/proxy to follow execution

}

private fun StringBuilder.newline() = appendLine()

private fun StringBuilder.glslVersion(): StringBuilder = appendLine("#version 330 core") //TODO

private fun StringBuilder.glslUniforms(uniforms: List<ShaderBuilder.UniformStatement>) = appendLine(
    uniforms
        .filter { it.used }
        .joinToString("\n") { "uniform ${it.type.glslName} ${it.name};" })

private fun StringBuilder.glslStage(stage: ShaderStage): StringBuilder =
    appendLine("#pragma stage ${stage.name.lowercase()}")

private fun StringBuilder.glslVertexAttributes(attributes: Map<String, KClass<*>>): StringBuilder =
    append(attributes.map { (name, type) ->
        "in ${type.glslName} $name;"
    }.joinToString("\n", postfix = "\n"))

private fun StringBuilder.glslStruct(type: KClass<*>, members: Map<String, KClass<*>>): StringBuilder =
    appendLine(buildString {
        appendLine("struct ${type.simpleName} {")
        appendLine(
            members
                .map { (name, type) -> "\t${type.glslName} $name;" }
                .joinToString("\n"))
        append("};")
    })

private fun StringBuilder.glslMain() = appendLine("void main() {\n}")

private enum class GlslStorageQualifier { IN, OUT }

private fun StringBuilder.glslStatement(qualifier: GlslStorageQualifier, type: KClass<*>, name: String) = appendLine(
    "${qualifier.name.lowercase()} ${type.simpleName} $name;"
)

private fun StringBuilder.glslRenderTargets(renderTargets: Map<String, KClass<*>>) = appendLine(
    renderTargets
        .map { (name, _) -> "out vec4 $name;" }
        .joinToString("\n"))

@ShaderDslMarker
open class GlslContext(val shader: ShaderBuilder<*, *, *>) {
    fun vec4(x: float, y: float, z: float, w: float): vec4 = register({ Vector4f(x, y, z, w) }, "vec4", x, y, z, w)
    fun vec4(vec: vec3, w: float): vec4 = register({ Vector4f(vec, w) }, "vec4", vec, w)

    private inline fun <reified T : Any> register(value: () -> T, name: String, vararg args: Any): T = shader.frame {
        shader.programStatements.add(MethodProgramStatement(T::class, shader.callDepth, name, args.toList()))
        shader.proxy(value(), "instance")
    }

    fun code(@Language("GLSL") code: String): Unit = TODO("log call and return string wrapper proxy")
}

@ShaderDslMarker
class VertexReceiver(builder: ShaderBuilder<*, *, *>) : GlslContext(builder)

@ShaderDslMarker
class FragmentReceiver(builder: ShaderBuilder<*, *, *>) : GlslContext(builder)

@OptIn(ExperimentalStdlibApi::class)
internal val Any.id
    get() = "${this::class.qualifiedName}@${
        System.identityHashCode(this).toHexString()
    }" //TODO find out if the id hash code is even reliable / for how long

private fun <T : Any> ShaderBuilder<*, *, *>.proxyObject(clazz: KClass<T>, subject: String): T =
    proxy(clazz.java.newInstance(), subject)

private fun <T : Any> ShaderBuilder<*, *, *>.proxy(obj: T, subject: String): T {
    //ALRIGHT, so do not use reified types to get the classes for this
    return ByteBuddy()
        .subclass(obj::class.java)
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of { _, method, args ->
            //TODO add toggle to exclude calls outside of program block?

            if (method.name in listOf("hashCode", "toString")) {
                return@of method.invoke(obj, *(args ?: emptyArray()))
            }

            val returnValue = frame {
                programStatements.add(
                    MethodProgramStatement(
                        method.returnType.kotlin,
                        callDepth,
                        method.name,
                        (args ?: emptyArray()).toList()
                    )
                )
                method.invoke(obj, *(args ?: emptyArray()))
            }

            when (returnValue) {
//                returnValue::class.isFinal -> returnValue
                is Int -> returnValue
//                is Long -> returnValue
                is Float -> returnValue
//                is Double -> returnValue
                is Boolean -> returnValue
//                is Byte -> returnValue
//                is Char -> returnValue
                is String -> returnValue
                else -> proxy(returnValue, subject)
            }
        })
        .make()
        .load(obj::class.java.classLoader)
        .loaded
        .newInstance() //TODO primitive arg detection & instantiation
}

interface FrameBuffer
class RenderTarget(
    vector: vec4,
    var frameBuffer: FrameBuffer? = null,
) : vec4(vector) {
    infix fun set(frameBuffer: FrameBuffer) {
        this.frameBuffer = frameBuffer
    }
}

val vec4.rt: RenderTarget //FIXME kinda ugly, maybe just turn em into delegates like with uniforms
    get() = RenderTarget(this) //TODO potentially add program entry as well

internal inline fun <T> ShaderBuilder<*, *, *>.frame(block: () -> T): T {
    callDepth++
    val value = block()
    callDepth--
    return value
}

fun main() {
    val shader = TestShader()

    shader.vertexData = Vertex(vec3(1f, 2f, 3f), vec2(0f, 1f))
    shader.model = mat4()
}

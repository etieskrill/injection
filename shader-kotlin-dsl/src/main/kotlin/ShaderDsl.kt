package io.github.etieskrill.extension.shader.dsl

import io.github.etieskrill.extension.shader.dsl.ShaderBuilder.MethodProgramStatement
import io.github.etieskrill.extension.shader.dsl.ShaderBuilder.UniformStatement
import io.github.etieskrill.extension.shader.dsl.inner.TestShader
import io.github.etieskrill.extension.shader.dsl.inner.Vertex
import io.github.etieskrill.injection.extension.shader.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.intellij.lang.annotations.Language
import org.joml.Vector4f
import java.io.File
import java.lang.reflect.Method
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

private typealias ProxyLookup = MutableMap<String, Any>

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
    shader: AbstractShader
) : AbstractShader by shader {

    var vertexData: VA? = null

    internal val proxyLookup: ProxyLookup = mutableMapOf()

    private val vertexAttributes: VA = proxyObject(vertexAttributesClass, "data", proxyLookup)
    private val vertex: V = proxyObject(vertexClass, "data", proxyLookup)

    private val vertexAttributeStatements = mutableMapOf<String, KClass<*>>()
    private val vertexDataStatements = mutableMapOf<String, KClass<*>>()
    private val renderTargetStatements = mutableMapOf<String, KClass<*>>()

    internal var interceptProxyCalls = false
    private var generationDone = false

    internal var stage = ShaderStage.NONE
    internal var callDepth = -1

    private val uniformStatements = mutableMapOf<String, UniformStatement>()

    internal val programStatements = mutableListOf<ProgramStatement>()

    internal data class UniformStatement(
        val type: KClass<*>,
        val proxy: Any,
        var name: String? = null
    ) {
        val used: Boolean
            get() = name != null
    }

    internal open class ProgramStatement(
        open val callDepth: Int,
        open val stage: ShaderStage
    )

    private data class UniformProgramStatement(
        val uniform: UniformStatement,
        override val callDepth: Int,
        override val stage: ShaderStage
    ) : ProgramStatement(callDepth, stage)

    internal data class MethodProgramStatement(
        val returnType: KClass<*>,
        val returnValue: Any,
        override val callDepth: Int,
        override val stage: ShaderStage,
        val method: Method?,
        val name: String?,
        val `this`: Any?,
        val args: List<*>
    ) : ProgramStatement(callDepth, stage)

    internal data class ReturnValueProgramStatement(
        val value: Any,
        override val callDepth: Int,
        override val stage: ShaderStage
    ) : ProgramStatement(callDepth, stage)

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
        private var value: T = proxyObject(clazz, "uniform", proxyLookup)

        init {
            this@ShaderBuilder.uniformStatements[value.id] = UniformStatement(
                clazz,
                value
            ) //using the system object id, because hashCode logically returns identical values for identical matrices
        }

        override fun getValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>): T =
            if (!generationDone) programGetter(property)
            else getter()

        private fun programGetter(property: KProperty<*>): T {
            val uniformStatement = this@ShaderBuilder.uniformStatements[value.id]!!
            if (!uniformStatement.used) uniformStatement.name = property.name
            this@ShaderBuilder.programStatements.add(
                UniformProgramStatement(
                    uniformStatement,
                    callDepth,
                    ShaderStage.NONE
                )
            )
            return value
        }

        private fun getter(): T = TODO("AbstractShader is gonna have to grow some getters")

        override fun setValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>, value: T) {
            check(generationDone) { "Uniforms must not be set in program block" }
            this.value =
                value //property is only writeable to allow uniform to actually be set from outside without requiring another field
            setUniform(property.name, value)
        }
    }

    protected inline fun <reified T : Any> uniform() = UniformDelegate(T::class)

    protected fun vertex(block: VertexReceiver.(VA) -> V) {
        stage = ShaderStage.VERTEX
        interceptProxyCalls = true
        val vertex = block(VertexReceiver(this), vertexAttributes)
        programStatements.add(ReturnValueProgramStatement(vertex, callDepth, ShaderStage.VERTEX))
        interceptProxyCalls = false
    }

    protected fun fragment(block: FragmentReceiver.(V) -> RT) {
        stage = ShaderStage.FRAGMENT
        interceptProxyCalls = true
        val renderTargets = block(FragmentReceiver(this), vertex)
        programStatements.add(ReturnValueProgramStatement(renderTargets, callDepth, ShaderStage.FRAGMENT))
        interceptProxyCalls = false
    }

//    init {
//        program()
//        check(callDepth == -1) { "Not all proxied function calls returned. This... should not happen." }
//    }

    protected abstract fun program()

    internal fun generateGlsl() {
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
            glslMain(
                programStatements
                    .filter { it.stage == ShaderStage.VERTEX }
                    .filter { it.callDepth <= 0 },
                uniformStatements, proxyLookup
            )
            newline()

            glslStage(ShaderStage.FRAGMENT)
            newline()
            glslStatement(GlslStorageQualifier.IN, vertexClass, "vertex")
            newline()
            glslRenderTargets(renderTargetStatements)
            newline()
            glslMain(listOf(), uniformStatements, proxyLookup)
        }

        File("Test.glsl").writeText(source)

        generationDone = true
    }

}

private fun StringBuilder.newline() = appendLine()

private fun StringBuilder.glslVersion(): StringBuilder = appendLine("#version 330 core") //TODO

private fun StringBuilder.glslUniforms(uniforms: List<UniformStatement>) = appendLine(
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

private fun StringBuilder.glslMain(
    statements: List<ShaderBuilder.ProgramStatement>,
    uniformStatements: Map<String, UniformStatement>,
    proxyLookup: ProxyLookup
) = appendLine(buildString {
    appendLine("void main() {")

    val variableCounters = mutableMapOf<KClass<*>, Int>()
    val variables = mutableMapOf<Any, Pair<String, KClass<*>>>()
    var callDepth = 1 //we start in main
    val statement = statements
        .take(1)
        .map { statement ->
            when (statement) {
                is MethodProgramStatement -> {
                    val helperVariableName =
                        statement.returnType.glslName + "_" + variableCounters
                            .compute(statement.returnType) { _, counter ->
                                return@compute if (counter == null) 0 else counter + 1
                            }

                    variables[statement.returnValue] = helperVariableName to statement.returnType

                    check(statement.method != null || statement.name != null) { "Either method or method name must be set" }
                    val operator = (statement.method?.name ?: statement.name)!!.glslOperator
                        ?: TODO("Function calls are not yet implemented")
                    check(statement.`this` != null) { "Operators must have a left-hand value" }
                    check(statement.args.size == 2) { "Operators must have exactly one argument" } //FIXME a bit risky to assume all methods would have other + receiver parameters

                    val thisUniformName = uniformStatements[proxyLookup[statement.`this`.id]!!.id]!!.name
                    val argUniformName = uniformStatements[statement.args[0]!!.id]!!.name
                    "${"\t".repeat(callDepth)}${statement.returnType.glslName} $helperVariableName = $thisUniformName $operator $argUniformName;"
                }

                is ShaderBuilder.ReturnValueProgramStatement -> error("")
                else -> error("Unknown program statement type: ${statement::class.simpleName}")
            }
        }
    appendLine(statement.joinToString("\n"))

    append("}")
})

private val String.glslOperator
    get() = when (this) {
        "mul" -> "*"
        else -> null
    }

//private infix fun Any?.refEquals(other: Any?) =
//    this != null && other != null &&
//    System.identityHashCode(this) == System.identityHashCode(other)

//private data class MethodSignature

private enum class GlslStorageQualifier { IN, OUT }

private fun StringBuilder.glslStatement(qualifier: GlslStorageQualifier, type: KClass<*>, name: String) = appendLine(
    "${qualifier.name.lowercase()} ${type.simpleName} $name;"
)

private fun StringBuilder.glslRenderTargets(renderTargets: Map<String, KClass<*>>) = appendLine(
    renderTargets
        .map { (name, _) -> "out vec4 $name;" }
        .joinToString("\n"))

@ShaderDslMarker
open class GlslContext(
    private val shader: ShaderBuilder<*, *, *>,
    private val stage: ShaderStage
) {
    fun vec4(x: float, y: float, z: float, w: float): vec4 = register({ Vector4f(x, y, z, w) }, "vec4", x, y, z, w)
    fun vec4(vec: vec3, w: float): vec4 = register({ Vector4f(vec, w) }, "vec4", vec, w)

    private inline fun <reified T : Any> register(value: () -> T, name: String, vararg args: Any): T = shader.frame {
        val proxy = shader.proxy(value(), "instance", shader.proxyLookup)
//        val method = GlslContext::class.java.getMethod(name, *args.map { it::class.java }.toTypedArray()) //this seems like a pretty shit idea
        shader.programStatements.add(
            MethodProgramStatement(
                T::class,
                proxy,
                shader.callDepth,
                stage,
                null,
                name,
                null,
                args.toList()
            )
        )
        proxy
    }

    fun <T> code(@Language("GLSL") code: String): T = TODO("log call and return wrapper proxy")
}

@ShaderDslMarker
class VertexReceiver(builder: ShaderBuilder<*, *, *>) : GlslContext(builder, ShaderStage.VERTEX)

@ShaderDslMarker
class FragmentReceiver(builder: ShaderBuilder<*, *, *>) : GlslContext(builder, ShaderStage.FRAGMENT)

@OptIn(ExperimentalStdlibApi::class)
internal val Any.id
    get() = "${this::class.qualifiedName}@${
        System.identityHashCode(this).toHexString()
    }"

private fun <T : Any> ShaderBuilder<*, *, *>.proxyObject(
    clazz: KClass<T>,
    subject: String,
    proxyLookup: ProxyLookup
): T = proxy(clazz.java.newInstance(), subject, proxyLookup)

private fun <T : Any> ShaderBuilder<*, *, *>.proxy(obj: T, subject: String, proxyLookup: ProxyLookup): T {
    val proxy = ByteBuddy()
        .subclass(obj::class.java) //ALRIGHT, so do *NOT* use reified types to get the classes for this
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of { _, method, args ->
            if (!interceptProxyCalls) {
                return@of method.invoke(obj, *(args ?: emptyArray()))
            }

            frame {
                var returnValue = method.invoke(obj, *(args ?: emptyArray()))

                returnValue = when (returnValue) {
//                returnValue::class.isFinal -> returnValue
                    is Int -> returnValue
//                is Long -> returnValue
                    is Float -> returnValue
//                is Double -> returnValue
                    is Boolean -> returnValue
//                is Byte -> returnValue
//                is Char -> returnValue
                    is String -> returnValue
                    else -> proxy(returnValue, subject, proxyLookup)
                }

                programStatements.add(
                    MethodProgramStatement(
                        obj::class,
                        returnValue,
                        callDepth,
                        stage,
                        method,
                        null,
                        obj,
                        (args ?: emptyArray()).toList()
                    )
                )

                returnValue
            }
        })
        .make()
        .load(obj::class.java.classLoader)
        .loaded
        .newInstance() //TODO primitive arg detection & instantiation

    proxyLookup[obj.id] = proxy
    return proxy as T
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
//    shader.model = mat4()
}

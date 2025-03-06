package io.github.etieskrill.extension.shader.dsl

import io.github.etieskrill.extension.shader.dsl.ShaderBuilder.MethodProgramStatement
import io.github.etieskrill.extension.shader.dsl.inner.TestShader
import io.github.etieskrill.extension.shader.dsl.inner.Vertex
import io.github.etieskrill.injection.extension.shader.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.joml.Vector4f
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
    vertexAttributesClass: KClass<VA>,
    vertexClass: KClass<V>,
    renderTargetsClass: KClass<RT>,
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

    private data class UniformStatement(
        val type: KClass<*>,
        val proxy: Any,
        var name: String? = null
    ) {
        val used: Boolean
            get() = name == null
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
        vertexAttributeStatements.putAll(vertexAttributesClass.getFields())
        vertexDataStatements.putAll(vertexClass.getFields())
        renderTargetStatements.putAll(renderTargetsClass.getFields())
    }

    private fun KClass<*>.getFields(): Map<String, KClass<*>> = members
        .filterNot { it.name in listOf("equals", "hashCode", "toString") }
        .filterIsInstance<KMutableProperty<*>>()
        .associate { it.name to it.returnType.classifier as KClass<*> }

    protected inner class UniformDelegate<T : Any>(clazz: KClass<T>) : ReadWriteProperty<ShaderBuilder<VA, V, RT>, T> {
        private var value: T = proxyObject(clazz, "uniform")

        init {
            this@ShaderBuilder.uniformStatements.put(value, UniformStatement(clazz, value))
        }

        override fun getValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>): T {
            //TODO use flag to *actually* switch behaviour to get the value from shader (or cached) when outside of program definition
            val uniformStatement = this@ShaderBuilder.uniformStatements[value]!!
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
//    }

    protected abstract fun program()

    //TODO map identityHashCode to instance/proxy to follow execution

}

@ShaderDslMarker
open class GlslContext(val shader: ShaderBuilder<*, *, *>) {
    fun vec4(x: float, y: float, z: float, w: float): vec4 = register({ Vector4f(x, y, z, w) }, "vec4", x, y, z, w)
    fun vec4(vec: vec3, w: float): vec4 = register({ Vector4f(vec, w) }, "vec4", vec, w)

    private inline fun <reified T : Any> register(value: () -> T, name: String, vararg args: Any): T = shader.frame {
        shader.programStatements.add(MethodProgramStatement(T::class, shader.callDepth, name, args.toList()))
        shader.proxy(value(), "instance")
    }
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

private fun <T : Any> ShaderBuilder<*, *, *>.proxy(obj: T, subject: String): T =
    //ALRIGHT, so do not use reified types to get the classes for this
    ByteBuddy()
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

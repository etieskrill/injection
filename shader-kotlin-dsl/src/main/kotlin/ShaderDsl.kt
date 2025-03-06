package io.github.etieskrill.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.*
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.joml.Vector4f
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
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

    init {
        println("vertex attributes:")
        vertexAttributesClass.members
            .filterNot { it.name == "equals" || it.name == "hashCode" || it.name == "toString" }
            .joinToString(separator = "\n") { "\t${it.returnType} ${it.name}" }
            .run { println(this) }
        println("vertex data fields:")
        vertexClass.members
            .filterNot { it.name == "equals" || it.name == "hashCode" || it.name == "toString" }
            .joinToString(separator = "\n") { "\t${it.returnType} ${it.name}" }
            .run { println(this) }
        println("render targets:")
        renderTargetsClass.members
            .filterNot { it.name == "equals" || it.name == "hashCode" || it.name == "toString" }
            .joinToString(separator = "\n") { "\t${it.returnType} ${it.name}" }
            .run { println(this) }
    }

    @ShaderDslMarker
    protected inner class UniformDelegate<T : Any>(clazz: KClass<T>) : ReadWriteProperty<ShaderBuilder<VA, V, RT>, T> {
        private var value: T = proxyObject(clazz, "uniform")

        init {
            println("registered uniform: ${clazz.qualifiedName}")
        }

        override fun getValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>): T {
            println("called uniform getter: ${property.name}")
            return value
        }

        override fun setValue(thisRef: ShaderBuilder<VA, V, RT>, property: KProperty<*>, value: T) {
            println("called uniform setter: ${property.name} to $value")
            this.value = proxy(value, "uniform")
        }
    }

    protected inline fun <reified T : Any> uniform() = UniformDelegate(T::class)

    protected open class GlslContext {
        fun vec4(x: float, y: float, z: float, w: float): vec4 =
            register({ Vector4f(x, y, z, w) }, "constructor vec4($x, $y, $z, $w)")

        fun vec4(vec: vec3, w: float): vec4 = register({ Vector4f(vec, w) }, "constructor vec4(${vec.id}, ${w})")

        private fun <T : Any> register(value: () -> T, subject: String): T = frame {
            println("${"\t".repeat(depth)}called inline $subject (call depth: $depth)")
            proxy(value(), "instance")
        }
    }

    @ShaderDslMarker
    protected object VertexReceiver : GlslContext()

    protected fun vertex(block: VertexReceiver.(VA) -> V) {
        val vertex = block(VertexReceiver, vertexAttributes) //FIXME whäi dis no work
        println("produced vertex: $vertex")
    }

    @ShaderDslMarker
    protected object FragmentReceiver : GlslContext()

    protected fun fragment(block: FragmentReceiver.(V) -> RT) {
        val renderTargets = block(FragmentReceiver, vertex)
        println("produced render targets: $renderTargets")
    }

//    init {
//        program()
//    }

    protected abstract fun program()

    //TODO map identityHashCode to instance/proxy to follow execution

}

@OptIn(ExperimentalStdlibApi::class)
internal val Any.id
    get() = "${this::class.qualifiedName}@${
        System.identityHashCode(this).toHexString()
    }" //TODO find out if the id hash code is even reliable / for how long

var depth = -1

private fun <T : Any> proxyObject(clazz: KClass<T>, subject: String): T = proxy(clazz.java.newInstance(), subject)

private fun <T : Any> proxy(obj: T, subject: String): T =
    //ALRIGHT, so do not use reified types to get the classes for this
    ByteBuddy()
        .subclass(obj::class.java)
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of { _, method, args ->
            val returnValue = frame {
                println(
                    "${"\t".repeat(depth)}called $subject method ${method.name}(${
                        args?.joinToString { it.id }.orEmpty()
                    }) (call depth: $depth)"
                )
                method.invoke(obj, *(args ?: emptyArray()))
            }
            when (returnValue) {
//                returnValue::class.isFinal -> returnValue
                is Int -> returnValue
//                is Long -> returnValue
                is Float -> returnValue
//                is Double -> returnValue
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
    get() = frame {
        println("created render target: $id")
        RenderTarget(this)
    }

internal inline fun <T> frame(block: () -> T): T {
    depth++
    val value = block()
    depth--
    return value
}

fun main() {
    val shader = TestShader()

    shader.vertexData = Vertex(vec3(1f, 2f, 3f), vec2(0f, 1f))
    shader.model = mat4()
}

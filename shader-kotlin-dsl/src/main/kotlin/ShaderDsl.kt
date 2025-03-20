package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.*
import org.joml.Vector4f
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@DslMarker
@Target(AnnotationTarget.CLASS)
internal annotation class ShaderDslMarker

interface ShaderVertexData { //TODO omfg i forgot about the goddamn gl_Position... yes me, this is why e2e tests are also important
    val position: vec4
}

/**
 * @param VA the vertex attributes
 * @param V the internal vertex type
 * @param RT the render targets
 */
@ShaderDslMarker
abstract class ShaderBuilder<VA : Any, V : ShaderVertexData, RT : Any>(shader: AbstractShader) :
    AbstractShader by shader {

    protected fun <T : Any> uniform() = UniformDelegate<T>()

    protected fun <T : Any> const(value: T) = ConstDelegate<T>()

    protected fun vertex(block: VertexReceiver.(VA) -> V) {}

    protected fun fragment(block: FragmentReceiver.(V) -> RT) {}

    protected abstract fun program()

    protected fun vec2(x: Number, y: Number): vec2 = empty()

}

private fun empty(): Nothing = error("don't actually call these dingus")

//TODO maybe anonymous builders
//fun <VA: Any, V : ShaderVertexData, RT : Any> shaderBuilder(
//    shader: AbstractShader,
//    block: ShaderBuilder<VA, V, RT>.() -> Unit?
//): AbstractShader {
//} //since uniforms won't be accessible via property, is this even that useful?

class UniformDelegate<T : Any> :
    ReadWriteProperty<ShaderBuilder<*, *, *>, T> { //FIXME would have to be protected and internal... has this really never been a usecase?
    override fun getValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>): T =
        TODO("AbstractShader is gonna have to grow some getters")

    override fun setValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>, value: T) =
        thisRef.setUniform(property.name, value)
}

class ConstDelegate<T : Any> : ReadOnlyProperty<ShaderBuilder<*, *, *>, T> {
    override fun getValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>): T {
        TODO()
    }
}

@ShaderDslMarker
open class GlslReceiver {
    fun vec2(x: Number, y: Number): vec2 = empty()

    fun vec4(x: Number): vec4 = empty()
    fun vec4(x: Number, y: Number, z: Number, w: Number): vec4 = empty()
    fun vec4(vec: vec3, w: Number): vec4 = empty()
    fun vec4(vec: vec2, z: Number, w: Number): vec4 = empty()

    fun max(v1: vec2, v2: vec2): vec2 = empty()
}

@ShaderDslMarker
class VertexReceiver : GlslReceiver() {
    val vertexID: int = empty()
}

@ShaderDslMarker
class FragmentReceiver : GlslReceiver()

internal val properties = mapOf(
    ShaderStage.VERTEX to mapOf(
        VertexReceiver::vertexID.name to "gl_VertexID"
    )
)

internal val functionNames = listOf("vec2", "vec4", "max")

interface FrameBuffer
class RenderTarget(
    vector: vec4,
    var frameBuffer: FrameBuffer? = null,
) : Vector4f(vector) {
    infix fun set(frameBuffer: FrameBuffer) {
        this.frameBuffer = frameBuffer
    }
}

val vec4.rt: RenderTarget //FIXME kinda ugly, maybe just turn em into delegates like with uniforms
    get() = RenderTarget(this)

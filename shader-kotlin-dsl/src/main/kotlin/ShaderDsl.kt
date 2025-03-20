package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.joml.Vector4f
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

    protected fun vertex(block: VertexReceiver.(VA) -> V) {}

    protected fun fragment(block: FragmentReceiver.(V) -> RT) {}

    protected abstract fun program()

}

class UniformDelegate<T : Any> :
    ReadWriteProperty<ShaderBuilder<*, *, *>, T> { //FIXME would have to be protected and internal... has this really never been a usecase?
    override fun getValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>): T =
        TODO("AbstractShader is gonna have to grow some getters")

    override fun setValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>, value: T) =
        thisRef.setUniform(property.name, value)
}

@ShaderDslMarker
open class GlslReceiver {
    fun vec4(x: float, y: float, z: float, w: float): vec4 = Vector4f(x, y, z, w)
    fun vec4(vec: vec3, w: float): vec4 = Vector4f(vec, w)
}

@ShaderDslMarker
class VertexReceiver : GlslReceiver()

@ShaderDslMarker
class FragmentReceiver : GlslReceiver()

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

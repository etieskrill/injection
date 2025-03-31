package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.*
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface ShaderVertexData {
    val position: vec4
}

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

/**
 * @param VA the vertex attributes
 * @param V the internal vertex type
 * @param RT the render targets
 */
@ShaderDslMarker
@Suppress("unused")
abstract class ShaderBuilder<VA : Any, V : ShaderVertexData, RT : Any>(shader: AbstractShader) :
    AbstractShader by shader {

    protected fun <T : Any> uniform() = UniformDelegate<T>() //TODO add default param

    protected fun <T : Any> const(value: T) = ConstDelegate<T>() //TODO pass GlslReceiver block for dsl instead

    protected fun <T> func(block: context(GlslReceiver) () -> T): T = error()
    protected fun <T> vertFunc(block: context(VertexReceiver) (VA) -> T): T = error()
    protected fun <T> fragFunc(block: context(FragmentReceiver) (V) -> T): T = error()

    protected fun vertex(block: context(VertexReceiver) (VA) -> V) {}

    protected fun fragment(block: context(FragmentReceiver) (V) -> RT) {}

    protected abstract fun program()

    protected fun vec2(s: Number): vec2 = Vector2f()
    protected fun vec2(x: Number, y: Number): vec2 = Vector2f()
    protected fun ivec2(s: Number): ivec2 = Vector2i()

}

@ShaderDslMarker
@Suppress("UnusedReceiverParameter", "unused")
open class GlslReceiver {
    operator fun Number.div(v: vec2): vec2 = error()
    operator fun Number.div(v: ivec2): vec2 = error()

    fun vec2(s: Number): vec2 = Vector2f()
    fun vec2(x: Number, y: Number): vec2 = error()
    fun vec2(v: ivec2): vec2 = error()

    fun ivec2(s: Int): ivec2 = Vector2i()

    fun vec3(s: Number): vec3 = error()
    fun vec3(x: Number, y: Number, z: Number): vec3 = error()

    fun vec4(x: Number): vec4 = error()
    fun vec4(x: Number, y: Number, z: Number, w: Number): vec4 = error()
    fun vec4(v: vec3, w: Number): vec4 = error()
    fun vec4(v: vec2, z: Number, w: Number): vec4 = error()

    var vec2.x: float by swizzle()
    var vec2.y: float by swizzle()

    val vec4.rgb: vec3 by swizzle()

    operator fun vec2.plus(v: vec2): vec2 = error()
    operator fun vec2.minus(s: Number): vec2 = error()
    operator fun vec2.minus(v: vec2): vec2 = error()
    operator fun vec2.times(s: Number): vec2 = error()
    operator fun vec2.times(v: vec2): vec2 = error()
    operator fun vec2.times(v: ivec2): vec2 = error()
    operator fun vec2.div(v: vec2): vec2 = error()
    operator fun vec2.rem(s: Number): vec2 = error()

    operator fun ivec2.div(v: ivec2): ivec2 = error()

    operator fun vec3.plus(v: vec3): vec3 =
        error() //do NOT use the assignment operators (e.g. plusAssign) - great, so += MAY be automatically converted to + and =

    operator fun vec3.minus(v: vec3): vec3 = error()
    operator fun vec3.unaryMinus(): vec3 = error()
    operator fun vec3.times(v: vec3): vec3 = error()
    operator fun vec3.times(s: Number): vec3 = error()
    operator fun vec3.div(v: vec3): vec3 = error()

    fun max(v: vec2, s: Number): vec2 = error()
    fun max(v1: vec2, v2: vec2): vec2 = error()

    fun pow(v1: vec3, v2: vec3): vec3 = error()
    fun exp(s: Number): float = error()
    fun exp(v: vec3): vec3 = error()

    fun texture(sampler: sampler2D, coordinates: vec2): vec4 = error()
    fun textureSize(sampler: sampler2D, lod: int/* = 0*/): ivec2 =
        error() //FIXME default values never present in IrValueParameter for some reason
}

@ShaderDslMarker
class VertexReceiver : GlslReceiver() {
    val vertexID: int = error()
}

@ShaderDslMarker
class FragmentReceiver : GlslReceiver()

private fun error(): Nothing = error("don't actually call these dingus")

//TODO maybe anonymous builders
//fun <VA: Any, V : ShaderVertexData, RT : Any> shaderBuilder(
//    shader: AbstractShader,
//    block: ShaderBuilder<VA, V, RT>.() -> Unit?
//): AbstractShader {
//} //since uniforms won't be accessible via property, is this even that useful?

class UniformDelegate<T : Any> :
    ReadWriteProperty<ShaderBuilder<*, *, *>, T> { //FIXME would have to be protected and internal... has this really never been a usecase?
    var initialised = false

    override fun getValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>): T =
        TODO("AbstractShader is gonna have to grow some getters")

    override fun setValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>, value: T) {
        if (!initialised) { //TODO for getter also i guess?
            thisRef.addUniform(property.name, (property.returnType.classifier as KClass<*>).javaObjectType)
            initialised = true
        }
        when (value) { //TODO move to specific array delegate class and overload builder function
            is Array<*> -> thisRef.setUniformArray(property.name, value as Array<Any>)
            is Texture -> thisRef.setTexture(property.name, value)
            else -> thisRef.setUniform(property.name, value)
        }
    }
}

class ConstDelegate<T : Any> : ReadOnlyProperty<ShaderBuilder<*, *, *>, T> {
    override fun getValue(thisRef: ShaderBuilder<*, *, *>, property: KProperty<*>): T {
        error("Should not interact with shader const values")
    }
}

private fun <T, R> swizzle() = SwizzleDelegate<T, R>()

private class SwizzleDelegate<T, R> : ReadWriteProperty<T, R> {
    override fun getValue(thisRef: T, property: KProperty<*>): R =
        TODO("Not yet implemented")

    override fun setValue(thisRef: T, property: KProperty<*>, value: R): Unit =
        TODO("Not yet implemented")
}

@DslMarker
@Target(AnnotationTarget.CLASS)
internal annotation class ShaderDslMarker

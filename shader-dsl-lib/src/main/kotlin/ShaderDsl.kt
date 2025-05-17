package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.Texture
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.int
import io.github.etieskrill.injection.extension.shader.ivec2
import io.github.etieskrill.injection.extension.shader.mat2
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.samplerCube
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
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
@Suppress("unused", "UNUSED_PARAMETER")
abstract class ShaderBuilder<VA : Any, V : ShaderVertexData, RT : Any>(val shader: AbstractShader) :
    AbstractShader by shader {

    protected fun <T : Any> uniform() = UniformDelegate<T>() //TODO add default param

    protected fun <T : Number> const(value: T) = ConstDelegate<T>() //TODO pass GlslReceiver block for dsl instead
    protected fun const(value: Boolean) = ConstDelegate<Boolean>()
    protected fun <T : Number> const(value: Array<T>) = ConstDelegate<Array<T>>()
    protected fun const(value: Array<Boolean>) = ConstDelegate<Array<Boolean>>()
    protected fun const(value: vec2) = ConstDelegate<vec2>()
    protected fun const(value: Array<vec2>) = ConstDelegate<Array<vec2>>()
    protected fun const(value: ivec2) = ConstDelegate<ivec2>()
    protected fun const(value: Array<ivec2>) = ConstDelegate<Array<ivec2>>()
    protected fun const(value: vec3) = ConstDelegate<vec3>()
    protected fun const(value: Array<vec3>) = ConstDelegate<Array<vec3>>()
    protected fun const(value: vec4) = ConstDelegate<vec4>()
    protected fun const(value: Array<vec4>) = ConstDelegate<Array<vec4>>()

    protected fun <T> func(block: context(GlslReceiver) () -> T): T = error()
    protected fun <T> vertFunc(block: context(VertexReceiver) (VA) -> T): T = error()
    protected fun <T> fragFunc(block: context(FragmentReceiver) (V) -> T): T = error()

    protected fun vertex(block: context(VertexReceiver) (VA) -> V) {}

    protected fun fragment(block: context(FragmentReceiver) (V) -> RT) {}

    protected abstract fun program()

    //TODO use block with GlslReceiver context in const and remove these
    protected fun vec2(s: Number): vec2 = Vector2f()
    protected fun vec2(x: Number, y: Number): vec2 = Vector2f()
    protected fun ivec2(s: Number): ivec2 = Vector2i()

    protected fun vec3(x: Number, y: Number, z: Number): vec3 = Vector3f()

}

/**
 * "**Pure**" as in; takes no vertex inputs.
 */
abstract class PureShaderBuilder<V : ShaderVertexData, RT : Any>(shader: AbstractShader) :
    ShaderBuilder<Nothing, V, RT>(shader)

@ShaderDslMarker
@Suppress("unused", "UNUSED_PARAMETER")
open class GlslReceiver {
    operator fun Number.plus(s: Number): Number = error()
    operator fun Number.plus(v: vec2): vec2 = error()
    operator fun Number.minus(s: Number): Number = error()
    operator fun Number.minus(v: vec2): vec2 = error()
    operator fun Number.times(s: Number): Number = error()
    operator fun Number.times(v: vec2): vec2 = error()
    operator fun Number.times(v: vec3): vec3 = error()
    operator fun Number.rem(n: Number): Number = error()
    operator fun Number.unaryMinus(): Number = error()
    operator fun Number.compareTo(s: Number): Int = error()

    operator fun Number.div(v: vec2): vec2 = error()
    operator fun Number.div(v: ivec2): vec2 = error()

    fun vec2(s: Number): vec2 = Vector2f()
    fun vec2(x: Number, y: Number): vec2 = error()
    fun vec2(v: vec2): vec2 = error()
    fun vec2(v: ivec2): vec2 = error()

    fun ivec2(s: Int): ivec2 = Vector2i()
    fun ivec2(v: vec2): ivec2 = error()

    fun vec3(s: Number): vec3 = error()
    fun vec3(x: Number, y: Number, z: Number): vec3 = error()
    fun vec3(v: vec2, z: Number): vec3 = error()
    fun vec3(v: vec3): vec3 = error()
    fun vec3(v: vec4): vec3 = error()

    fun vec4(x: Number): vec4 = error()
    fun vec4(x: Number, y: Number, z: Number, w: Number): vec4 = error()
    fun vec4(v: vec3, w: Number): vec4 = error()
    fun vec4(v: vec2, z: Number, w: Number): vec4 = error()

    fun mat2(m00: Number, m01: Number, m10: Number, m11: Number): mat2 = error()

    var vec2.x: Number by swizzle()
    var vec2.y: Number by swizzle()
    var vec2.xy: vec2 by swizzle()
    var vec2.yx: vec2 by swizzle()
    var vec2.yy: vec2 by swizzle()

    var vec3.x: Number by swizzle()
    var vec3.y: Number by swizzle()
    var vec3.z: Number by swizzle()
    var vec3.xy: vec2 by swizzle()
    var vec3.xz: vec2 by swizzle()

    var vec4.x: float by swizzle()
    var vec4.y: float by swizzle()
    var vec4.z: float by swizzle()
    var vec4.w: float by swizzle()
    var vec4.xz: vec2 by swizzle()
    var vec4.xyz: vec3 by swizzle()
    val vec4.rgb: vec3 by swizzle()
    var vec4.a: float by swizzle()

    operator fun vec2.plus(v: vec2): vec2 = error()
    operator fun vec2.minus(s: Number): vec2 = error()
    operator fun vec2.minus(v: vec2): vec2 = error()
    operator fun vec2.times(s: Number): vec2 = error()
    operator fun vec2.times(v: vec2): vec2 = error()
    operator fun vec2.times(v: ivec2): vec2 = error()
    operator fun vec2.div(s: Number): vec2 = error()
    operator fun vec2.div(v: vec2): vec2 = error()
    operator fun vec2.div(v: ivec2): vec2 = error()
    operator fun vec2.rem(s: Number): vec2 = error()

    operator fun ivec2.rem(s: int): ivec2 = error()

    operator fun ivec2.div(v: vec2): vec2 = error()
    operator fun ivec2.div(v: ivec2): ivec2 = error()

    operator fun vec3.plus(v: vec3): vec3 =
        error() //do NOT use the assignment operators (e.g. plusAssign) - great, so += MAY be automatically converted to + and =

    operator fun vec3.plus(v: Number): vec3 = error()
    operator fun vec3.minus(v: Number): vec3 = error()
    operator fun vec3.minus(v: vec3): vec3 = error()
    operator fun vec3.unaryMinus(): vec3 = error()
    operator fun vec3.times(v: vec3): vec3 = error()
    operator fun vec3.times(s: Number): vec3 = error()
    operator fun vec3.div(v: vec3): vec3 = error()
    operator fun vec3.rem(s: Number): vec3 = error()

    operator fun vec4.plus(s: Number): vec4 = error()
    operator fun vec4.plus(v: vec4): vec4 = error()
    operator fun vec4.minus(v: vec4): vec4 = error()
    operator fun vec4.times(v: Number): vec4 = error()
    operator fun vec4.times(v: vec4): vec4 = error()

    operator fun mat2.times(v: vec2): vec2 = error()

    operator fun mat4.times(v: vec4): vec4 = error()

    fun normalize(v: vec3): vec3 = error()

    fun cross(v1: vec3, v2: vec3): vec3 = error()

    fun length(v: vec2): float = error()
    fun length(v: vec3): float = error()

    fun dot(v1: vec2, v2: vec2): float = error()
    fun dot(v1: vec3, v2: vec3): float = error() //TODO do with receivers

    fun sqrt(s: Number): float = error()

    fun max(a: Number, b: Number): Number = error()

    fun min(a: Number, b: Number): Number = error()
    fun max(v: vec2, s: Number): vec2 = error()
    fun max(v1: vec2, v2: vec2): vec2 = error()

    fun pow(v1: vec3, v2: vec3): vec3 = error()
    fun exp(s: Number): float = error()
    fun exp(v: vec3): vec3 = error()

    fun sin(s: Number): float = error()

    fun cos(n: Number): float = error()
    fun acos(n: Number): float = error()

    fun smoothstep(a: Number, b: Number, t: Number): float = error()
    fun smoothstep(a: Number, b: Number, t: vec2): vec2 = error()
    fun smoothstep(a: vec2, b: vec2, t: Number): vec2 = error()
    fun smoothstep(a: vec2, b: vec2, t: vec2): vec2 = error()

    fun abs(s: Number): float = error()
    fun abs(v: vec2): vec2 = error()
    fun abs(v: vec3): vec3 = error()

    fun floor(n: Number): float = error()
    fun floor(n: vec2): vec2 = error()
    fun floor(n: vec3): vec3 = error()
    fun floor(n: vec4): vec4 = error()

    fun clamp(x: Number, min: Number, max: Number): float = error()

    fun fract(s: Number): float = error()
    fun fract(s: vec2): vec2 = error()
    fun fract(s: vec3): vec3 = error()

    fun mix(a: Number, b: Number, t: Number): float = error()
    fun mix(a: vec4, b: vec4, t: Number): vec4 = error()

    fun texture(sampler: sampler2D, coordinates: vec2): vec4 = error()
    fun texture(sampler: samplerCube, coordinates: vec3): vec4 = error()

    fun textureSize(sampler: sampler2D, lod: int/* = 0*/): ivec2 =
        error() //FIXME default values never present in IrValueParameter for some reason

    fun inverse(m: mat4): mat4 = error()

}

@ShaderDslMarker
class VertexReceiver : GlslReceiver() {
    val vertexID: int = error()
}

@ShaderDslMarker
@Suppress("unused", "UNUSED_PARAMETER")
class FragmentReceiver : GlslReceiver() {
    fun dFdx(s: Number): float = error()
    fun dFdy(s: Number): float = error()
    fun fwidth(v: Number): float = error()
    fun fwidth(v: vec2): vec2 = error()
}

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

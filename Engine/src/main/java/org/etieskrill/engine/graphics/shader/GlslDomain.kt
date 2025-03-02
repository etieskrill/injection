package org.etieskrill.engine.graphics.shader

import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.model.Vertex
import org.etieskrill.engine.graphics.model.VertexAccessor
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * @param VIN the input vertex data type
 * @param V the internal vertex data type
 * @param RT the render targets
 */
abstract class ShaderBuilder<VIN, V, RT>(
    private val vertexAccessor: VertexArrayAccessor<VIN>
) {
    private val mockVertex: VIN by TODO()
    private val mockVertexData: V by TODO()
    private val mockRenderTargets: RT by TODO()

    private val uniforms: MutableList<UniformDelegate<*>> = mutableListOf()
    private val renderTargets: MutableList<RenderTarget<*>> = mutableListOf()

    private var mutableSource: StringBuilder
    val source = mutableSource.toString()

    protected inner class UniformDelegate<T : Any>(
        val clazz: KClass<T>
    ) : ReadWriteProperty<ShaderBuilder<VIN, V, RT>, T> {
        private lateinit var value: T

        override fun getValue(thisRef: ShaderBuilder<VIN, V, RT>, property: KProperty<*>): T {
            thisRef.uniforms += this
            return this as T
        }

        override fun setValue(thisRef: ShaderBuilder<VIN, V, RT>, property: KProperty<*>, value: T) {
        }
    }

    protected inline fun <reified T : Any> uniform() = UniformDelegate(T::class)

    protected inner class RenderTarget<T : FrameBufferAttachment>(
        val attachment: T
    ) : vec4(), ReadWriteProperty<ShaderBuilder<VIN, V, RT>, vec4> {
        override fun getValue(thisRef: ShaderBuilder<VIN, V, RT>, property: KProperty<*>): vec4 =
            throw IllegalStateException("Render target values may not be gotten")

        override fun setValue(thisRef: ShaderBuilder<VIN, V, RT>, property: KProperty<*>, value: vec4) {
            TODO("Not yet implemented")
        }
    }

//    inline fun <reified T : Any> renderTarget() = RenderTarget(T::class)

    fun vertex(block: VertexDomain<VIN>.() -> V) = block(VertexDomain(mockVertex))
    fun fragment(block: FragmentDomain<V>.() -> RT) = block(FragmentDomain(mockVertexData))

    abstract fun program()
}

abstract class GlslDomain {
    fun vec3(vec4: vec4) = vec3(vec4.x(), vec4.y(), vec4.z())

    fun normalize(vector: Vector3f): vec3 = TODO()
}

class VertexDomain<T>(val vertex: T) : GlslDomain()
class FragmentDomain<T>(val vertex: T) : GlslDomain()

typealias vec2 = Vector2f
typealias vec3 = Vector3f
typealias vec4 = Vector4f

typealias mat3 = Matrix3f
typealias mat4 = Matrix4f

typealias sampler2D = Texture2D

class TestShader :
    ShaderBuilder<Vertex, TestShader.VertexData, TestShader.RenderTargets>(VertexAccessor.getInstance()) {
    var mesh by uniform<mat4>()
    var model by uniform<mat4>()
    var combined by uniform<mat4>()
    var normal by uniform<mat3>()

    val textureScale by uniform<vec2>()

    val lightCombined by uniform<mat4>()

    data class VertexData(
        val position: vec4,
        val normal: vec3,
        val tbn: mat3,
        val texCoord: vec2,
        val fragPos: vec3,
        val lightSpaceFragPos: vec4
    )

    data class RenderTargets(
        val colour: RenderTarget<sampler2D>,
        val bloom: RenderTarget<sampler2D>
    )

    override fun program() {
        vertex {
            val normalVec = normalize(normal * vertex.normal!!)
            val tangent = normalize(normal * vertex.tangent!!)
            val biTangent = normalize(normal * vertex.biTangent!!)
            val tbn = mat3(normalVec, tangent, biTangent)

            val fragPos = vec3(model * mesh * vec4(vertex.position, 1f))

            VertexData(
                combined * model * mesh * vec4(vertex.position, 1f),
                normalVec, tbn,
                vertex.textureCoords!! * textureScale,
                fragPos,
                lightCombined * vec4(fragPos, 1f)
            )
        }
        fragment {
            RenderTargets(
                colour = vec4(1f, 0f, 0f, 1f),
                bloom = vec4(0f)
            )
        }
    }
}

fun main() {
    val shader = TestShader()
    println(shader.source)
//    shader.model = Matrix4f()
//    val model = shader.model
}

package org.etieskrill.engine.entity.service.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.mat3
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMagFilter
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.joml.Matrix3f
import org.joml.Matrix4f
import kotlin.math.PI

class DeferredRenderService(
    private val renderer: Renderer,
    screenBuffer: FrameBuffer,
    private val camera: Camera
) : Service {

    private val gPosition = Texture2D(
        screenBuffer.size, format = TextureFormat.RGB, type = TextureType.G_POSITION,
        minFilter = TextureMinFilter.NEAREST, magFilter = TextureMagFilter.NEAREST
    )
    private val gColour = Texture2D(
        screenBuffer.size, format = TextureFormat.RGB, type = TextureType.G_COLOUR,
        minFilter = TextureMinFilter.NEAREST, magFilter = TextureMagFilter.NEAREST
    )
    private val gNormal = Texture2D(
        screenBuffer.size, format = TextureFormat.RGB, type = TextureType.G_NORMAL,
        minFilter = TextureMinFilter.NEAREST, magFilter = TextureMagFilter.NEAREST
    )
    private val gDepth = Texture2D(
        screenBuffer.size, format = TextureFormat.DEPTH, type = TextureType.G_DEPTH,
        minFilter = TextureMinFilter.NEAREST, magFilter = TextureMagFilter.NEAREST
    )

    private val gBuffer = FrameBuffer(
        screenBuffer.size, mapOf(
            FrameBufferAttachmentType.COLOUR0 to gPosition,
            FrameBufferAttachmentType.COLOUR1 to gColour,
            FrameBufferAttachmentType.COLOUR2 to gNormal,
            FrameBufferAttachmentType.DEPTH to gDepth
        )
    )

    private val gBufferShader = GBufferShader()

    private val deferredShader = DeferredShader()
    private val deferredPipeline = PostPassPipeline(deferredShader, screenBuffer)

    override fun canProcess(entity: Entity) = entity.hasComponents<Transform, Drawable>()

    override fun preProcess(delta: Double, entities: List<Entity>) = gBuffer.clear()

    override fun process(
        targetEntity: Entity,
        entities: List<Entity>,
        delta: Double
    ) {
        // deferred:
        // - opaque
        // - decals
        // - skybox
        // - finish deferred
        // forward:
        // - transparent (+ decals?)
        // post:
        // - particles (or earlier?)
        // - colour & brightness ramp
        // - bloom, shake, vignette etc.

        val transform = targetEntity.getComponent<Transform>()!!
        val drawable = targetEntity.getComponent<Drawable>()!!

        gBufferShader.view = camera.view
        gBufferShader.combined = camera.combined

        drawable.model.nodes.forEach { node ->
            val nodeTransform = (node.getGlobalTransform() * transform).matrix
            val nodeNormalTransform = nodeTransform.normal(Matrix3f())
            gBufferShader.apply {
                this.mesh = Matrix4f()
                model = nodeTransform
                normal = nodeNormalTransform
            }

            node.meshes.forEach { mesh ->
                //TODO acceleration structure?? maybe it's just... fine? GCs were made for this after all
                val pipeline = Pipeline(mesh.vao, PipelineConfig(), gBufferShader, gBuffer)
                renderer.render(pipeline)
            }
        }
    }

    override fun postProcess(entities: List<Entity>) {
        deferredShader.gPosition = gPosition
        deferredShader.gColour = gColour
        deferredShader.gNormal = gNormal
        deferredShader.gDepth = gDepth

        deferredShader.viewPosition = camera.viewPosition

        renderer.render(deferredPipeline)
    }


}

class GBufferShader() :
    ShaderBuilder<GBufferShader.Vertex, GBufferShader.VertexData, GBufferShader.RenderTargets>(
        object : Shader(listOf("shaders/GBuffer.glsl")) {}
    ) {
    data class Vertex(val position: vec3, val normalVec: vec3)
    data class VertexData(override val position: vec4, val worldPosition: vec4, val normal: vec3) : ShaderVertexData
    data class RenderTargets(val gPosition: vec3, val gColour: vec3, val gNormal: vec3)

    var mesh: mat4 by uniform()
    var model: mat4 by uniform()
    var normal: mat3 by uniform()
    var view: mat4 by uniform()
    var combined: mat4 by uniform()

    override fun program() {
        vertex {
            val worldPosition = model * (mesh * vec4(it.position, 1))
            val fragPosition = combined * worldPosition
            VertexData(fragPosition, worldPosition, normal * it.normalVec)
        }
        fragment {
            RenderTargets(vec3(it.worldPosition), vec3(0.6), it.normal)
        }
    }
}

class DeferredShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : Shader(listOf("shaders/Deferred.glsl"), false) {}
) {
    private val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var gPosition: sampler2D by uniform()
    var gColour: sampler2D by uniform()
    var gNormal: sampler2D by uniform()
    var gDepth: sampler2D by uniform()

    var viewPosition: vec3 by uniform()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val texCoords = it.position.xy / 2 + 0.5

            val position = texture(gPosition, texCoords).xyz
            val fragColour = texture(gColour, texCoords).rgb
            val normal = texture(gNormal, texCoords).xyz
            val depth = texture(gDepth, texCoords).x

            val ambient = 0.2

            val yaw = PI / 5f
            val pitch = PI / 5f
            val sinA = sin(yaw)
            val cosA = cos(yaw)
            val sinB = sin(pitch)
            val cosB = cos(pitch)
            val lightPos = mat3(
                cosA * cosB, -sinA, cosA * sinB,
                sinA * cosB, cosA, sinA * sinB,
                -sinB, 0, cosB
            ) * normalize(viewPosition)
            val diffuse = (1 - ambient) * max(0, dot(lightPos, normal))

            ColourRenderTarget(vec4(fragColour * min(1, ambient + diffuse), 1))
        }
    }
}

package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Enabled
import org.etieskrill.engine.entity.component.PointLightComponent
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType
import org.etieskrill.engine.graphics.framebuffer.RenderBuffer
import org.etieskrill.engine.graphics.framebuffer.RenderBufferType
import org.etieskrill.engine.graphics.model.Skybox
import org.etieskrill.engine.graphics.particle.ParticleRenderer
import org.etieskrill.engine.graphics.pipeline.FillMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.pipeline.StencilMode
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.shader.impl.DilationOutlineShader
import org.etieskrill.engine.graphics.shader.impl.FullScreenColourShader
import org.etieskrill.engine.graphics.shader.impl.LightSourceShader
import org.etieskrill.engine.graphics.shader.impl.SkyboxShader
import org.etieskrill.engine.graphics.shader.impl.StaticShader
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.joml.Matrix3f
import org.joml.Vector2f
import org.joml.Vector2ic
import org.joml.Vector4ic
import kotlin.reflect.KClass

open class RenderService(
    private val frameBuffer: FrameBuffer,
    internal val renderer: Renderer,
    private val camera: Camera,
    internal val windowSize: Vector2ic,

    var cullingCamera: Camera = camera,
    private val customViewport: Vector4ic? = null,
    var skybox: Skybox? = null
) : Service {

    protected val _renderer: Renderer = renderer
    protected val _camera: Camera = camera

    protected val shader = StaticShader()
    protected val lightSourceShader = LightSourceShader()

    private val gaussBlurPostBuffers = GaussBlurPostBuffers(renderer, windowSize)
    val postEffectsFrameBuffer = gaussBlurPostBuffers.frameBuffer

    val hdrShader get() = gaussBlurPostBuffers.hdrShader

    private val skyboxShader = SkyboxShader()

    internal val outlineTexture = Texture2D(
        windowSize, TextureType.DIFFUSE, format = TextureFormat.SRGBA, wrapping = TextureWrapping.CLAMP_TO_BORDER
    )
    internal val outlineDepthStencilTexture = Texture2D(
        windowSize, TextureType.UNKNOWN, format = TextureFormat.DEPTH_STENCIL
    )
    private val outlineFrameBuffer = FrameBuffer(
        windowSize, mapOf(
            FrameBufferAttachmentType.COLOUR0 to outlineTexture,
            FrameBufferAttachmentType.DEPTH_STENCIL to outlineDepthStencilTexture
        )
    )

    internal val fullScreenPipeline = PostPassPipeline(
        FullScreenColourShader(), outlineFrameBuffer,
        opaque = false, depthTest = false, stencilMode = StencilMode.FILTER
    )
    internal val outlinePipeline = PostPassPipeline(
        DilationOutlineShader(), postEffectsFrameBuffer,
        opaque = false, depthTest = false, stencilMode = StencilMode.FILTER_NOT
    )

    var blur = true

    private val shaderParams = ShaderParams()

    //TODO remove jury-rigged service
    // - "inner services"?
    // - service groups?
    // - global render state -> context as entity?
    @Deprecated("just don't")
    val boundingBoxRenderService = BoundingBoxRenderService(frameBuffer, renderer, camera)

    @Deprecated("just don't")
    val particleRenderService = ParticleRenderService(ParticleRenderer(renderer.context), camera)

    @Deprecated("man")
    private var lastDelta = 0.0

    private data class ShaderParams(
        val uniformBindings: MutableMap<String, Any> = mutableMapOf(),
        val uniformArrayBindings: MutableMap<String, Array<Any>> = mutableMapOf(),
        val textureBindings: MutableMap<String, Texture> = mutableMapOf(),
        val configuredShaders: MutableSet<Shader> = mutableSetOf()
    )

    override fun canProcess(entity: Entity) = entity.hasComponents<Transform, Drawable>()

    override fun preProcess(delta: Double, entities: List<Entity>) {
        outlineFrameBuffer.clear()

        //TODO either revert to previously bound framebuffer, or use dsa

        postEffectsFrameBuffer.clear()

        shaderParams.apply {
            uniformBindings.clear()
            uniformArrayBindings.clear()
            textureBindings.clear()
            configuredShaders.clear()
        }

        skybox?.let {
            TODO("render skybox")
//            renderer.render(skybox)
        }

        for (entity in entities) {
            //TODO expand to multiple directional lights
            val directionalLightComponent = entity.getComponent<DirectionalLightComponent>() ?: continue
            val hasShadowMap = directionalLightComponent.shadowMap != null
            shaderParams.apply {
                uniformBindings["hasShadowMap"] = hasShadowMap
                if (hasShadowMap) {
                    textureBindings["shadowMap"] = directionalLightComponent.shadowMap.texture
                    uniformBindings["lightCombined"] = directionalLightComponent.camera!!.combined
                }
                uniformArrayBindings["globalLights"] = arrayOf(directionalLightComponent.directionalLight)
            }
            break
        }

        val pointLightComponents = entities.mapNotNull { it.getComponent<PointLightComponent>() }

        if (pointLightComponents.isNotEmpty()) {
            shaderParams.uniformArrayBindings["lights"] = pointLightComponents
                .map { it.light }
                .toTypedArray()
        }

        val shadowMaps = pointLightComponents.mapNotNull { it.shadowMap }.distinct()
        if (shadowMaps.isNotEmpty()) {
            shaderParams.uniformArrayBindings["pointShadowMaps"] = shadowMaps.toTypedArray()
        }
        shaderParams.uniformBindings["hasPointShadowMaps"] = shadowMaps.isNotEmpty()

        shaderParams.uniformBindings["viewPosition"] = camera.viewPosition

        entities.forEach { entity ->
            if (boundingBoxRenderService.canProcess(entity)) {
                boundingBoxRenderService.process(entity, entities, 0.0)
            }
        }
    }

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        if (targetEntity.getComponent<Enabled>()?.enabled?.not() ?: false) return

        val drawable = targetEntity.getComponent<Drawable>()!!
        if (!drawable.isVisible) return

        val boundingBox = targetEntity.getComponent<WorldSpaceAABB>()
        if (boundingBox != null && !cullingCamera.frustumTestAABB(boundingBox)) {
            return
        }

        val transform = targetEntity.getComponent<Transform>()!!

        drawable.model.nodes.forEach { node ->
            val modelTransform = transform * node.transform
            val normalMatrix = modelTransform.matrix.normal(Matrix3f())
            node.meshes.forEach { mesh ->
                val shader = getConfiguredShader(targetEntity, drawable)

                val pipeline = Pipeline(
                    mesh.vao,
                    PipelineConfig(
                        fillMode = if (drawable.isWireframeEnabled) FillMode.LINE else FillMode.FILL,
                        stencilMode = if (drawable.isOutlineEnabled) StencilMode.SET_FRONT else StencilMode.OFF
                    ),
                    shader,
                    postEffectsFrameBuffer
                )

                pipeline.shader.apply {
                    if ("model" in uniforms) setUniform("model", modelTransform.matrix)
                    if ("normalMat" in uniforms) setUniform("normalMat", normalMatrix)
                    if ("combined" in uniforms) setUniform("combined", camera.combined)
                }

                renderer.render(pipeline)
            }
        }

        lastDelta = delta
    }

    protected fun getConfiguredShader(entity: Entity, drawable: Drawable): Shader {
        drawable.shader?.let {
            configureShader(it, shaderParams)
            return it
        }

        val directionalLightComponent = entity.getComponent<DirectionalLightComponent>()
        val pointLightComponent = entity.getComponent<PointLightComponent>()
        if (directionalLightComponent != null) {
            lightSourceShader.setUniform("light", directionalLightComponent.directionalLight)
            return lightSourceShader
        } else if (pointLightComponent != null) {
            lightSourceShader.setUniform("light", pointLightComponent.light)
            pointLightComponent.shadowFarPlane?.let {
                shader.setUniform("pointShadowFarPlane", it) //TODO make per-light?
            }
            return lightSourceShader
        } else {
            configureShader(shader, shaderParams)
            drawable.textureScale?.let { shader.setUniform("textureScale", it) }
            return shader
        }
    }

    private fun configureShader(shader: Shader, params: ShaderParams) {
        if (shader in params.configuredShaders) return

        params.uniformBindings.forEach { (name, value) -> if (name in shader.uniforms) shader.setUniform(name, value) }
        params.uniformArrayBindings.forEach { (name, value) ->
            if (name in shader.uniformArrays) shader.setUniformArray(name, value)
        }
        params.textureBindings.forEach { (name, texture) ->
            if (name in shader.uniforms) shader.setTexture(name, texture)
        }
    }

    override fun postProcess(entities: List<Entity>) {
        entities.forEach { entity -> //FIXME particle rendering is suddenly really fucking slow for some reason
            if (particleRenderService.canProcess(entity)) {
                particleRenderService.process(entity, entities, lastDelta)
            }
        }

        drawOutlines()

        gaussBlurPostBuffers.renderToScreen(frameBuffer, blur, customViewport)
    }

    override val runAfter: Set<KClass<out Service>>
        get() = setOf(DirectionalShadowMappingService::class, PointShadowMappingService::class)

}

internal expect fun RenderService.drawOutlines()

class GaussBlurPostBuffers(val renderer: Renderer, val windowSize: Vector2ic) {

    companion object {
        private const val GAUSS_BLUR_ITERATIONS = 3
    }

    private val hdrBuffer = Texture2D.createBlank(windowSize, TextureFormat.RGBA_HDR)
    private val bloomBuffer = Texture2D.createBlank(windowSize, TextureFormat.RGBA_HDR)
    val frameBuffer = FrameBuffer(windowSize, mapOf(
        FrameBufferAttachmentType.COLOUR0 to hdrBuffer,
        FrameBufferAttachmentType.COLOUR1 to bloomBuffer,
        FrameBufferAttachmentType.DEPTH_STENCIL to RenderBuffer(windowSize, RenderBufferType.DEPTH_STENCIL)
    ))

    private val blurTextureBuffer1 = Texture2D.createBlank(windowSize, TextureFormat.RGBA_HDR)
    private val blurFrameBuffer1 = FrameBuffer(windowSize, mapOf(FrameBufferAttachmentType.COLOUR0 to blurTextureBuffer1))
    private val blurTextureBuffer2 = Texture2D.createBlank(windowSize, TextureFormat.RGBA_HDR)
    private val blurFrameBuffer2 = FrameBuffer(windowSize, mapOf(FrameBufferAttachmentType.COLOUR0 to blurTextureBuffer2))

    private val gaussBlurShader = GaussBlurShader()
    private val blurPipeline1 = PostPassPipeline(gaussBlurShader, blurFrameBuffer1, depthTest = false)
    private val blurPipeline2 = PostPassPipeline(gaussBlurShader, blurFrameBuffer2, depthTest = false)

    val hdrShader = HDRShader()

    fun renderToScreen(screenBuffer: FrameBuffer, blur: Boolean, customViewport: Vector4ic?) {
        var blurBuffer1IsTarget = false

        if (blur) {
            blurFrameBuffer1.clear()
            blurFrameBuffer2.clear()

            gaussBlurShader.apply {
                source = bloomBuffer
                horizontal = true
                sampleDistance = Vector2f(2f)
            }

            renderer.render(blurPipeline1)

            repeat(GAUSS_BLUR_ITERATIONS * 2 - 1) {
                gaussBlurShader.apply {
                    source = if (blurBuffer1IsTarget) blurTextureBuffer2 else blurTextureBuffer1
                    horizontal = blurBuffer1IsTarget

                    renderer.render(if (blurBuffer1IsTarget) blurPipeline1 else blurPipeline2)

                    blurBuffer1IsTarget = !blurBuffer1IsTarget
                }
            }
        }

        val pipeline = PostPassPipeline(hdrShader, screenBuffer, opaque = false, depthTest = false)
        if (customViewport != null) {
            TODO("custom viewport")
        }

        hdrShader.apply {
            hdrBuffer = this@GaussBlurPostBuffers.hdrBuffer
            bloomBuffer = if (blur) {
                if (blurBuffer1IsTarget) blurTextureBuffer1 else blurTextureBuffer2
            } else {
                this@GaussBlurPostBuffers.bloomBuffer
            }
        }

        renderer.render(pipeline)
    }

}

//was only for debugging purposes, can maybe be reused for some visualisation
//class BoundingSphereRenderer implements Disposable {
//
//    private final Shader boundingSphereShader;
//    private final int dummyVao;
//
//    BoundingSphereRenderer(Shader boundingSphereShader) {
//        this.boundingSphereShader = new Shader(List.of("BoundingSphere.glsl"), false) {
//        };
//
//        this.dummyVao = glGenVertexArrays();
//    }
//
//    private void drawBoundingSpherePerspective(Camera cullingCamera, WorldSpaceAABB aabb) {
//        Vector4f position = cullingCamera.getCombined().transform(new Vector4f(aabb.center(new Vector3f()), 1));
//        position.x /= position.w;
//        position.y /= position.w;
//
//        //TODO play with joml env: joml.debug=true;joml.fastmath=true;joml.sinLookup=true
//        float boundRad = (aabb.getSize(new Vector3f()).length() / 2) / max(1, abs(position.z));
//
//        Vector2ic viewport = cullingCamera.getViewportSize();
//        float aspect = (float) viewport.x() / (float) viewport.y();
//
//        boundingSphereShader.setUniform("position", position);
//        boundingSphereShader.setUniform("radius", boundRad);
//
//        boundingSphereShader.setUniform("aspect", aspect);
//        boundingSphereShader.start();
//
//        glBindVertexArray(dummyVao);
//
//        glDisable(GL_DEPTH_TEST);
//        glDisable(GL_CULL_FACE);
//        glDepthMask(false);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        glDrawArrays(GL_POINTS, 0, 1);
//        glBlendFunc(GL_ONE, GL_ZERO);
//        glDepthMask(true);
//        glEnable(GL_CULL_FACE);
//        glEnable(GL_DEPTH_TEST);
//    }
//
//    @Override
//    public void dispose() {
//        boundingSphereShader.dispose();
//        glDeleteVertexArrays(dummyVao);
//    }
//
//}

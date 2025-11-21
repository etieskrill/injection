package io.github.etieskrill.games.leverage

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.PointLightComponent
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService
import org.etieskrill.engine.entity.service.impl.PointShadowMappingService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShader
import org.etieskrill.engine.graphics.model.CubeMapModel
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.pipeline.AlphaMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.util.Loaders
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.plus
import org.joml.primitives.AABBf
import org.joml.primitives.Intersectionf.intersectRayAab
import org.joml.primitives.Rayf
import org.joml.times

fun main() {
    App().run()
}

class App : org.etieskrill.engine.application.App(window {
    size = Window.WindowSize.LARGEST_FIT
    mode = Window.WindowMode.BORDERLESS
    samples = 4
    vSync = true
}) {

    val camera = PerspectiveCamera(window.currentSize).apply {
        setOrbit(true)
        setOrbitDistance(10f)
        setFar(50f)
    }

    val gizmoCamera = OrthographicCamera(Vector2i(1, 1))

    val rayPipeline = Pipeline(
        2,
        PipelineConfig(alphaMode = AlphaMode.SOURCE_ALPHA, primitiveType = PrimitiveType.LINES),
        LineShader(),
        null
    )

    var lastRay: Rayf? = null
    var selectedEntity: Entity? = null

    override fun init() {
        val shipModel = Loaders.ModelLoader.get().load("human-bb") { Model.ofFile("hooman-bb.glb") }

        entitySystem.createEntity()
            .withComponent(Transform())
            .addComponent(Drawable(shipModel))

        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(
                DirectionalLightComponent(
                    DirectionalLight(Vector3f(-1f), Vector3f(0f), Vector3f(1f), Vector3f(1f)),
                    DirectionalShadowMap.generate(Vector2i(2048)),
                    OrthographicCamera(Vector2i(2048), 20f, -20f, -20f, 20f).apply {
                        // FIXME i hate myself
                        setOrbit(true)
                        setOrbitDistance(10f)
                        setRotation(-45f, 90f + 45f, 0f)

                        near = -1f
                        far = 20f
                    }
                ))

        val engineNodes = arrayOf(
            "engine-plume-far-left",
            "engine-plume-left",
            "engine-plume-main",
            "engine-plume-right",
            "engine-plume-far-right"
        )

        val pointShadowMaps = PointShadowMapArray.generate(Vector2i(256), engineNodes.size)

        engineNodes.map { nodeName ->
            PointLight(
                shipModel.nodes.single { it.name == nodeName }.hierarchyTransform.position,
                Vector3f(0f),
                Vector3f(0f, 5f, 5f),
                Vector3f(0f, 1f, 1f),
                1f,
                .14f,
                .07f //TODO custom "volumetric" glow instead of bloom
            )
        }.forEachIndexed { index, light ->
            entitySystem.createEntity()
                .withComponent(Transform())
                .withComponent(
                    PointLightComponent(
                        light,
                        pointShadowMaps, index,
                        pointShadowMaps.getCombinedMatrices(0.1f, 20f, light), 20f
                    )
                )
        }

        entitySystem.addService(DirectionalShadowMappingService(renderer))
        entitySystem.addService(PointShadowMappingService(renderer, DepthCubeMapArrayShader()))
        entitySystem.addService(RenderService(renderer, camera, window.currentSize).apply {
            skybox = CubeMapModel("textures/cubemaps/space")
        })

        window.addKeyInputs(KeyCameraController(camera))
        val cursorCameraController = CursorCameraController(camera).apply { disable() }
        window.addCursorInputs(cursorCameraController)

        window.addCursorInputs(object : CursorInputAdapter {
            override fun invokeClick(button: Key?, action: Keys.Action?, posX: Double, posY: Double): Boolean {
                if (button!!.value == Keys.MIDDLE_MOUSE.input.value) {
                    when (action!!) {
                        Keys.Action.PRESS -> cursorCameraController.enable()
                        Keys.Action.RELEASE -> cursorCameraController.disable()
                        Keys.Action.REPEAT -> {}
                    }

                    return true
                }

                if (button.value == Keys.LEFT_MOUSE.input.value && action!! == Keys.Action.RELEASE) {
                    lastRay = camera.castViewportRay(posX.toInt(), posY.toInt())

                    entitySystem.entities.forEach {
                        val transform = it.getComponent<Transform>() ?: return@forEach
                        val drawable = it.getComponent<Drawable>() ?: return@forEach

                        val worldSpaceAABB = AABBf(drawable.model.boundingBox).transform(transform.matrix)

                        if (intersectRayAab(lastRay, worldSpaceAABB, Vector2f())) {
                            selectedEntity = it
                            drawable.isDrawOutline = true
                        } else {
                            selectedEntity = null
                            drawable.isDrawOutline = false
                        }

                        return true
                    }
                }

                return false
            }
        })

//        window.scene = Scene(
//            Batch(renderer, window.currentSize),
//            Container(TranslateGizmo {}),
//            gizmoCamera.apply {
//                near = -10000f
//                far = 10000f
//
//                setOrbit(true)
//                setOrbitDistance(10f)
//            }
//        )
    }

    override fun loop(delta: Double) {
        //FIXME ts makes peak no sense
        gizmoCamera.setRotation(camera.rotation)
        gizmoCamera.setRotation(camera.pitch, -camera.yaw, camera.roll)
    }

    override fun render() {
        //depth texture access reference
//        shadowMap.texture.bind()
//        glTexParameteri(shadowMap.texture.target.gl(), GL_TEXTURE_COMPARE_MODE, GL_NONE)
//
//        blitShadowShader.start()
//        blitShadowShader.size = Vector2f(300f)
//        blitShadowShader.sprite = shadowMap.texture
//        blitShadowShader.windowSize = Vector2f(window.currentSize)
//
//        glBindVertexArray(dummyVAO)
//        glViewport(0, 0, window.currentSize.x(), window.currentSize.y()) //TODO i hate global state
//        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
//
//        shadowMap.texture.bind()
//        glTexParameteri(shadowMap.texture.target.gl(), GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE)

        lastRay?.run {
            rayPipeline.shader.apply {
                pointA = Vector3f(oX, oY, oZ)
                pointB = Vector3f(oX, oY, oZ) + Vector3f(dX, dY, dZ) * 10f
                colour = Vector4f(1f, 0f, 0f, 0.5f)
                combined = camera.combined
            }
            renderer.render(rayPipeline)
        }
    }
}

class LineShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Line.glsl")) {}
) {
    var pointA by uniform<vec3>()
    var pointB by uniform<vec3>()

    var colour by uniform<vec4>()

    var combined by uniform<mat4>()

    override fun program() {
//        vertex { VertexData(vec4(if (vertexID == 0) pointA else pointB, 1)) } //FIXME ruh oh
        vertex {
            val point = if (vertexID == 0) pointA else pointB
            VertexData(combined * vec4(point, 1))
        }
        fragment { ColourRenderTarget(colour.rt) }
    }
}

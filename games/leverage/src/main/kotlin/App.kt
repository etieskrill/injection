package io.github.etieskrill.games.leverage

import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.PointLightComponent
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService
import org.etieskrill.engine.entity.service.impl.PointShadowMappingService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray
import org.etieskrill.engine.graphics.gl.shader.impl.BlitDepthShader
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShader
import org.etieskrill.engine.graphics.model.CubeMapModel
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.util.Loaders
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.opengl.GL30C.*

fun main() {
    App().run()
}

class App : GameApplication(window {
    size = Window.WindowSize.LARGEST_FIT
    mode = Window.WindowMode.BORDERLESS
    vSync = true
}) {

    private lateinit var shadowMap: DirectionalShadowMap
    private val blitShadowShader = BlitDepthShader()

    override fun init() {
        val shipModel = Loaders.ModelLoader.get().load("human-bb") { Model.ofFile("hooman-bb.glb") }

        val bbDrawable = entitySystem.createEntity()
            .withComponent(Transform())
            .addComponent(Drawable(shipModel).apply { isDrawOutline = true })
        window.addKeyInputs { type, key, action, modifiers ->
            if (key == Keys.Q.input.value && action == Keys.Action.RELEASE.glfwAction) {
                bbDrawable.isDrawOutline = !bbDrawable.isDrawOutline
                true
            }
            false
        }

        val camera = PerspectiveCamera(window.currentSize).apply {
            setOrbit(true)
            setOrbitDistance(10f)
            setFar(50f)
        }

        shadowMap = DirectionalShadowMap.generate(Vector2i(2048))

        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(
                DirectionalLightComponent(
                    DirectionalLight(Vector3f(-1f), Vector3f(0f), Vector3f(1f), Vector3f(1f)),
                    shadowMap,
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
        window.addCursorInputs(CursorCameraController(camera))

        window.cursor.disable()
    }

    override fun loop(delta: Double) {}

    private val dummyVAO = glGenVertexArrays()

    override fun render() {
        shadowMap.texture.bind()
        glTexParameteri(shadowMap.texture.target.gl(), GL_TEXTURE_COMPARE_MODE, GL_NONE)

        blitShadowShader.start()
        blitShadowShader.size = Vector2f(300f)
        blitShadowShader.sprite = shadowMap.texture
        blitShadowShader.windowSize = Vector2f(window.currentSize)

        glBindVertexArray(dummyVAO)
        glViewport(0, 0, window.currentSize.x(), window.currentSize.y()) //TODO i hate global state
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        shadowMap.texture.bind()
        glTexParameteri(shadowMap.texture.target.gl(), GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE)
    }
}

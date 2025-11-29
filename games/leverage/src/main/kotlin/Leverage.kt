package io.github.etieskrill.games.leverage

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService
import org.etieskrill.engine.entity.service.impl.PointShadowMappingService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShader
import org.etieskrill.engine.graphics.gl.shader.impl.GridShader
import org.etieskrill.engine.graphics.gl.shader.impl.LineShader
import org.etieskrill.engine.graphics.gl.shader.impl.ScreenSpacePointShader
import org.etieskrill.engine.graphics.gl.shader.impl.camera
import org.etieskrill.engine.graphics.gl.shader.impl.position
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.pipeline.AlphaMode
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.Input
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.container.Container
import org.etieskrill.engine.scene.element.Label
import org.etieskrill.engine.util.Loaders
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.minus
import org.joml.plus
import org.joml.primitives.AABBf
import org.joml.primitives.Intersectionf.intersectRayAab
import org.joml.primitives.Intersectionf.intersectRayPlane
import org.joml.primitives.Planef
import org.joml.primitives.Rayf
import org.joml.times
import org.joml.unaryMinus
import kotlin.math.floor

fun main() {
    Leverage().run()
}

class Leverage : org.etieskrill.engine.application.App(window {
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

    val cursorCameraController: CursorCameraController
    val cursorCameraTranslationController: CursorCameraTranslationController

    val gizmoCamera = OrthographicCamera(Vector2i(1, 1))

    val rayPipeline = Pipeline(
        2,
        PipelineConfig(alphaMode = AlphaMode.SOURCE_ALPHA, primitiveType = PrimitiveType.LINES),
        LineShader(),
        null
    )

    var lastRay: Rayf? = null
    var selectedEntity: Entity? = null

    val comPipeline = PostPassPipeline(ScreenSpacePointShader(), null, false, false)

    val statusLabel: Label

    val cursorPipeline = PostPassPipeline(BlitShader(), null, opaque = false, depthTest = false)
    val translateCursorImage = Texture2D.FileBuilder("textures/cursors/Hyper_Arrows_Move.png")
        .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST)
        .build()

    var transformMode = TransformMode.NONE

    enum class TransformMode {
        NONE, TRANSLATE, ROTATE, SCALE
    }

    val translateController = TranslateController(camera)

    val gridPipeline = Pipeline(
        1, PipelineConfig(
            primitiveType = PrimitiveType.POINTS,
            cullingMode = CullingMode.NONE,
            depthTest = false,
            writeDepth = false
        ), GridShader(), null
    )

    init {
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

//        engineNodes.map { nodeName ->
//            PointLight(
//                shipModel.nodes.single { it.name == nodeName }.hierarchyTransform.position,
//                Vector3f(0f),
//                Vector3f(0f, 5f, 5f),
//                Vector3f(0f, 1f, 1f),
//                1f,
//                .14f,
//                .07f //TODO custom "volumetric" glow instead of bloom
//            )
//        }.forEachIndexed { index, light ->
//            entitySystem.createEntity()
//                .withComponent(Transform())
//                .withComponent(
//                    PointLightComponent(
//                        light,
//                        pointShadowMaps, index,
//                        pointShadowMaps.getCombinedMatrices(0.1f, 20f, light), 20f
//                    )
//                )
//        }

        entitySystem.addService(DirectionalShadowMappingService(renderer))
        entitySystem.addService(PointShadowMappingService(renderer, DepthCubeMapArrayShader()))
        entitySystem.addService(RenderService(renderer, camera, window.currentSize).apply {
//            skybox = CubeMapModel("textures/cubemaps/space")
        })

        window.addKeyInputs(KeyCameraController(camera))
        cursorCameraController = CursorCameraController(camera).apply { disable() }
        window.addCursorInputs(cursorCameraController)

        cursorCameraTranslationController = CursorCameraTranslationController(camera).apply { enabled = false }
        window.addCursorInputs(cursorCameraTranslationController)

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

        window.addKeyInputs(
            Input.of(
                Input.bind(Keys.G).to { delta -> translate() },
                Input.bind(Keys.W).to { delta -> rotate() },
                Input.bind(Keys.S).to { delta -> scale() },
                Input.bind(Keys.ESC).to { delta -> cancel() }
            ))

        window.addCursorInputs(translateController)

        window.addKeyInputs { type, key, action, modifiers ->
            when (key) {
                Keys.CTRL.glfwKey -> {
                    if (action != Keys.Action.RELEASE.glfwAction) {
                        //TODO cursor wrapping
                        cursorCameraController.enable()
                    } else {
                        cursorCameraController.disable()
                    }
                }

                Keys.ALT.glfwKey -> {
                    cursorCameraTranslationController.enabled = action != Keys.Action.RELEASE.glfwAction
                }
            }

            false
        }

        statusLabel = Label()
        window.scene = Scene(
            Batch(renderer, window.currentSize),
            Container(statusLabel),
            OrthographicCamera(window.currentSize)
        )

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

    fun translate() {
        if (selectedEntity == null) return

        if (transformMode == TransformMode.TRANSLATE) {
            translateController.apply()
            cursorCameraController.enable()
            val cursorPosition = window.cursor.position
            window.cursor.enable()
            window.cursor.position = Vector2d(
                cursorPosition.x remActual window.currentSize.x().toDouble(),
                cursorPosition.y remActual window.currentSize.y().toDouble()
            )
            transformMode = TransformMode.NONE
            return
        }

        translateController.activate(selectedEntity!!.getComponent<Transform>()!!, window.cursor.position)
        transformMode = TransformMode.TRANSLATE

        cursorCameraController.disable()
        window.cursor.disable()
    }

    fun rotate() {
        if (selectedEntity == null) return
        transformMode = TransformMode.ROTATE

        cursorCameraController.disable()
    }

    fun scale() {
        if (selectedEntity == null) return
        transformMode = TransformMode.SCALE
    }

    fun cancel() {
        if (transformMode != TransformMode.NONE) {
            translateController.cancel()
            transformMode = TransformMode.NONE
            cursorCameraController.enable()
            window.cursor.enable()
            return
        }

        if (selectedEntity != null) {
            selectedEntity!!.getComponent<Drawable>()!!.isDrawOutline = false
            selectedEntity = null
        }
    }

    override fun loop(delta: Double) {
        //FIXME ts makes peak no sense
        gizmoCamera.setRotation(camera.rotation)
        gizmoCamera.setRotation(camera.pitch, -camera.yaw, camera.roll)

        statusLabel.text = """
             Selected entity: ${selectedEntity?.id ?: "N/A"}
             Transform mode: $transformMode
        """.trimIndent()
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

        selectedEntity?.let {
            comPipeline.shader.apply {
                val position = it.getComponent<Transform>()!!.position
                //erm aktshually this is the centroid, and not the centre of mass - stfu
                val centreOfMass = it.getComponent<Drawable>()!!.model.boundingBox.center(Vector3f())
                ndcPosition = camera.worldToView(position + centreOfMass).xy(Vector2f())
                aspectRatio = camera.aspectRatio
                size = 5f / camera.viewportSize.y()
                colour = Vector4f(1f, 0f, 0f, 1f)
            }
            renderer.render(comPipeline)
        }

        if (translateController.enabled) {
            cursorPipeline.shader.apply {
                sprite = translateCursorImage
                useSpriteColour = true

                val cursorSize = Vector2f(translateCursorImage.size)
                position = translateController.cursorPosition - cursorSize.div(2f, Vector2f())
                size = cursorSize

                windowSize = Vector2f(window.currentSize)
            }
            renderer.render(cursorPipeline)
        }

        gridPipeline.shader.position = Vector3f(0f)
        gridPipeline.shader.camera = camera
        renderer.render(gridPipeline)
    }
}

class TranslateController(val camera: Camera) : CursorInputAdapter {
    var enabled = false
        private set
    val cursorPosition = Vector2f()

    private var transform: Transform? = null

    private var first = true

    private var plane = Planef()
    private val originalPosition = Vector3f()
    private val originalCastPosition = Vector3f()

    fun activate(transform: Transform, cursorPosition: Vector2d) {
        enabled = true
        first = true
        this.transform = transform
        this.cursorPosition.set(cursorPosition)
    }

    fun apply() {
        if (!enabled) return

        enabled = false
        transform = null
    }

    fun cancel() {
        if (!enabled) return

        transform!!.position.set(originalPosition)

        enabled = false
        transform = null
    }

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        if (!enabled) return false
        check(transform != null) { "Transform is null" }

        if (first) {
            plane = Planef(transform!!.position, -camera.direction)
            originalPosition.set(transform!!.position)
        }

        val ray = camera.castViewportRay(posX.toInt(), posY.toInt())
        val t = intersectRayPlane(ray, plane, 0.001f)
        val pos = Vector3f(ray.oX, ray.oY, ray.oZ) + Vector3f(ray.dX, ray.dY, ray.dZ) * t

        if (first) {
            originalCastPosition.set(pos)
            first = false
        }

        transform!!.setPosition(originalPosition + pos - originalCastPosition)

        cursorPosition.set(
            posX remActual camera.viewportSize.x().toDouble(),
            posY remActual camera.viewportSize.y().toDouble()
        )

        return true
    }
}

class CursorCameraTranslationController(val camera: Camera) : CursorInputAdapter {
    var enabled = true
        set(value) {
            first = true
            field = value
        }

    private val prevPosition = Vector2f()
    private var first = true

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        if (!enabled) return false

        if (first) {
            prevPosition.set(posX, posY)
            first = false
        }

        camera.translate(Vector3f(posX.toFloat() - prevPosition.x, posY.toFloat() - prevPosition.y, 0f) / 100f)

        prevPosition.set(posX, posY)

        return true
    }
}

infix fun Double.remActual(other: Double) = (1 / other * this - floor(1 / other * this)) * other

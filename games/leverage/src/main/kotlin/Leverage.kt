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
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.mod as modStd

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
    val rotateController = RotateController(camera)
    val scaleController = ScaleController(camera)

    val gridPipeline = Pipeline(
        1, PipelineConfig(
            alphaMode = AlphaMode.SOURCE_ALPHA,
            primitiveType = PrimitiveType.POINTS,
            cullingMode = CullingMode.NONE,
            depthTest = false,
            writeDepth = false,
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
                Input.bind(Keys.G).to { delta -> transform(TransformMode.TRANSLATE, translateController) },
                Input.bind(Keys.R).to { delta -> transform(TransformMode.ROTATE, rotateController) },
                Input.bind(Keys.F).to { delta ->
                    transform(
                        TransformMode.SCALE,
                        scaleController
                    )
                }, //FIXME not needed for simple animation
                Input.bind(Keys.ESC).to { delta -> cancel() }
            ))

        window.addCursorInputs(translateController)
        window.addCursorInputs(rotateController)
        window.addCursorInputs(scaleController)

        window.addKeyInputs { type, key, action, modifiers ->
            if (transformMode != TransformMode.NONE) return@addKeyInputs false
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

    fun transform(mode: TransformMode, controller: TransformController) {
        if (selectedEntity == null) return

        if (transformMode == mode) {
            controller.apply()
            val cursorPosition = window.cursor.position
            window.cursor.enable()
            window.cursor.position = Vector2d(
                cursorPosition.x mod window.currentSize.x().toDouble(),
                cursorPosition.y mod window.currentSize.y().toDouble()
            )
            transformMode = TransformMode.NONE
            return
        }

        controller.activate(
            selectedEntity!!.getComponent<Transform>()!!,
            window.cursor.position,
            selectedEntity!!.getComponent<Drawable>()!!.model.boundingBox.center(Vector3f())
        )
        transformMode = mode

        cursorCameraController.disable()
        window.cursor.disable()
    }

    fun cancel() {
        if (transformMode != TransformMode.NONE) {
            when (transformMode) {
                TransformMode.TRANSLATE -> translateController.cancel()
                TransformMode.ROTATE -> rotateController.cancel()
                TransformMode.SCALE -> scaleController.cancel()
                else -> error("boat")
            }
            transformMode = TransformMode.NONE
            cursorCameraController.disable()
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
                val transform = it.getComponent<Transform>()!!
                //erm aktshually this is the centroid, and not the centre of mass - stfu
                val centreOfMass = it.getComponent<Drawable>()!!.model.boundingBox.center(Vector3f())
                ndcPosition = camera.worldToView(centreOfMass.mulPosition(transform.matrix)).xy(Vector2f())
                aspectRatio = camera.aspectRatio
                size = 5f / camera.viewportSize.y()
                colour = Vector4f(1f, 0f, 0f, 1f)
            }
            renderer.render(comPipeline)
        }

        listOf(translateController, rotateController, scaleController)
            .singleOrNull { it.enabled }
            ?.also {
                cursorPipeline.shader.apply {
                    sprite = translateCursorImage
                    useSpriteColour = true

                    val cursorSize = Vector2f(translateCursorImage.size)
                    position = it.cursorPosition - cursorSize.div(2f, Vector2f())
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

interface TransformController {
    val enabled: Boolean
    val cursorPosition: Vector2f

    fun activate(transform: Transform, cursorPosition: Vector2d, centreOfMass: Vector3f)
    fun apply()
    fun cancel()
}

class TranslateController(val camera: Camera) : CursorInputAdapter, TransformController {
    override var enabled = false
        private set
    override val cursorPosition = Vector2f()

    private var transform: Transform? = null

    private var first = true

    private var plane = Planef()
    private val originalPosition = Vector3f()
    private val originalCastPosition = Vector3f()

    override fun activate(transform: Transform, cursorPosition: Vector2d, centreOfMass: Vector3f) {
        enabled = true
        first = true
        this.transform = transform
        this.cursorPosition.set(cursorPosition)
    }

    override fun apply() {
        if (!enabled) return

        enabled = false
        transform = null
    }

    override fun cancel() {
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
            posX mod camera.viewportSize.x().toDouble(),
            posY mod camera.viewportSize.y().toDouble()
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

class RotateController(val camera: Camera) : CursorInputAdapter, TransformController {
    override var enabled: Boolean = false
        private set
    override var cursorPosition = Vector2f()

    private var transform: Transform? = null

    private val centreOfMass = Vector3f()
    private val viewCentreOfMass = Vector2f()
    private var originalAngle = 0f
    private val originalTransform = Transform()
    private val rotationAxis = Vector3f()

    override fun activate(transform: Transform, cursorPosition: Vector2d, centreOfMass: Vector3f) {
        this.transform = transform
        this.cursorPosition.set(cursorPosition)
        this.centreOfMass.set(centreOfMass)
        camera.worldToView(centreOfMass.mulPosition(transform.matrix))
            .xy(this.viewCentreOfMass).mul(1f, -1f)
            .div(2f).add(0.5f, 0.5f)
            .mul(Vector2f(camera.viewportSize))
        val delta = this.cursorPosition - this.viewCentreOfMass
        originalAngle = atan2(delta.y, delta.x)
        originalTransform.set(transform)
        rotationAxis.set(camera.direction)
        enabled = true
    }

    override fun apply() {
        if (!enabled) return

        transform = null
        enabled = false
    }

    override fun cancel() {
        if (!enabled) return

        transform!!.set(originalTransform)
        transform = null
        enabled = false
    }

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        if (!enabled) return false

        val transform = transform ?: error("Transform is null")

        val position = Vector2f(posX.toFloat(), posY.toFloat())
        val delta = position - viewCentreOfMass
        val angle = atan2(delta.y, delta.x) - originalAngle

        transform.set(originalTransform)
        val originalCOM = transform.matrix.transformPosition(centreOfMass, Vector3f())
        transform.rotation.rotateAxis(angle, rotationAxis)
        val deltaPos = transform.matrix.transformPosition(centreOfMass, Vector3f()) - originalCOM
        transform.translate(-deltaPos)

        cursorPosition = position mod camera.viewportSize

        return false
    }
}

class ScaleController(val camera: Camera) : CursorInputAdapter, TransformController {
    override var enabled = false
        private set
    override var cursorPosition = Vector2f()

    private var transform: Transform? = null

    private val originalPosition = Vector2f()
    private var originalDistance = 0f
    private var first = true

    private val originalScale = Vector3f()

    override fun activate(transform: Transform, cursorPosition: Vector2d, centreOfMass: Vector3f) {
        this.transform = transform
        enabled = true
        first = true
    }

    override fun apply() {
        transform = null
        enabled = false
    }

    override fun cancel() {
        transform = null
        enabled = false
    }

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        if (!enabled) return false
        val transform = transform ?: error("Transform is null")

        val position = Vector2f(posX.toFloat(), posY.toFloat())

        if (first) {
            originalPosition.set(camera.worldToView(transform.position))
            originalDistance = position.length()
            originalScale.set(transform.scale)
            first = false
        }

        var newScale = position.length() - originalDistance
        newScale = sign(newScale) * sqrt(abs(newScale / 1000f))
        transform.setScale(originalScale + Vector3f(newScale))

        cursorPosition = position mod camera.viewportSize

        return true
    }
}

infix fun Float.mod(other: Float) = modStd(other)
infix fun Double.mod(other: Double) = modStd(other)
infix fun Vector2fc.mod(other: Vector2fc) = Vector2f(x() mod other.x(), y() mod other.y())
infix fun Vector2fc.mod(other: Vector2ic) = Vector2f(x() mod other.x().toFloat(), y() mod other.y().toFloat())
infix fun Vector2f.modAssign(other: Vector2fc) = set(x mod other.x(), y mod other.y())
infix fun Vector2f.modAssign(other: Vector2ic) = set(x mod other.x().toFloat(), y mod other.y().toFloat())

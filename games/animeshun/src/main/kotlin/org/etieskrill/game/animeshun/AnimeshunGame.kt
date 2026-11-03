package org.etieskrill.game.animeshun

import io.github.oshai.kotlinlogging.KotlinLogging
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import net.bifreus.kml.deg
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.animation.AnimationBlendMode
import org.etieskrill.engine.graphics.animation.Animator
import org.etieskrill.engine.graphics.animation.NodeFilter
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.model.Node
import org.etieskrill.engine.graphics.model.SkyboxMaterial
import org.etieskrill.engine.graphics.model.loader.loadModel
import org.etieskrill.engine.graphics.model.loader.loadModelAnimations
import org.etieskrill.engine.graphics.shader.impl.AnimationShader
import org.etieskrill.engine.graphics.shader.impl.CubeMapShader
import org.etieskrill.engine.graphics.shader.impl.StaticShader
import org.etieskrill.engine.graphics.shader.impl.boneMatrices
import org.etieskrill.engine.graphics.shader.impl.combined
import org.etieskrill.engine.graphics.shader.impl.globalLights
import org.etieskrill.engine.graphics.shader.impl.showBoneSelector
import org.etieskrill.engine.graphics.shader.impl.showBoneWeights
import org.etieskrill.engine.graphics.shader.impl.viewPosition
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureType.DIFFUSE
import org.etieskrill.engine.input.InputTriggerEdge
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.ModifierKey
import org.etieskrill.engine.input.bindKey
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.inputs
import org.etieskrill.engine.util.EngineModelLoader
import org.etieskrill.engine.util.FixedArrayDeque
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.WindowSize
import kotlin.math.max

private val logger = KotlinLogging.logger {}

fun main() = AnimeshunGame.run()

object AnimeshunGame : App(Window(WindowSize.LARGEST_FIT, title = "Animeshun yeeees")) {

    private val camera = PerspectiveCamera(window.size)

    init {
        window.cursor.disable()
        window.scene = DebugOverlay(window.screenBuffer, renderer, textRenderer, pacer)

        window.cursorInputs += CursorCameraController(camera)
    }

    private val skybox = SkyboxMaterial("space", TextureCubeMap.createFromFile("textures/cubemaps/space", DIFFUSE))
    private val cubeMapShader = CubeMapShader()

    private val vampy = EngineModelLoader.load("vampy") {
        loadModel("models/mixamo_walk_forward_skinned_vampire.dae").apply {
//            .setInitialTransform(new Transform(
//                new Vector3f(0f, -1f, 0f),
//            new Quaternionf(),
//            new Vector3f(1)
//            ))
        }
    }
    private val vampyBB = EngineModelLoader.load("vampyBB") {
        loadModel("models/box.obj")
//        .setInitialTransform(new Transform(
//              new Vector3f(2.5f, -1f, 0f),
//              new Quaternionf(),
//              vampy.getBoundingBox().getSize(new Vector3f()).mul(.01f))
//        )
    }
    private val vampyTransform = Transform()

    private var boneSelector = 4
    private var showBoneWeights = false

    private val vampyPosDelta = Vec3()
    private val vampyShader = AnimationShader().also {
        it.showBoneSelector = boneSelector
        it.showBoneWeights = showBoneWeights
    }

    private val cube = EngineModelLoader.load("cube") { loadModel("models/cube.obj") }
    private val cubeTransform = Transform()
    private val shader = StaticShader()

    private val globalLight = DirectionalLight(Vec3(1f, -1f, 1f), Vec3(2f), Vec3(2f), Vec3(2f))

    private val vampyAnimator = Animator(vampy).apply {
        add(loadModelAnimations("models/mixamo_orc_idle.dae", vampy)[0])
        addNormalisedGroup(.8) {
            it.add(loadModelAnimations("models/mixamo_walking_forward.dae", vampy)[0])
            it.add(loadModelAnimations("models/mixamo_left_strafe_walking.dae", vampy)[0])
            it.add(loadModelAnimations("models/mixamo_right_strafe_walking.dae", vampy)[0])
            it.add(loadModelAnimations("models/mixamo_walking_backwards.dae", vampy)[0]) { it.playbackSpeed = 1.1f }
        }
        add(loadModelAnimations("models/mixamo_running.dae", vampy)[0]) { it.playbackSpeed = .7f }
        add(loadModelAnimations("models/mixamo_waving.dae", vampy)[0]) {
            it.blendMode = AnimationBlendMode.OVERRIDING

            val rightArmNode = vampy.rootNode.find("mixamorig_RightShoulder")!!
            it.filter = NodeFilter.tree(rightArmNode)
        }
        add(loadModelAnimations("models/mixamo_hip_hop_dancing.dae", vampy)[0])
    }

    private const val VAMPY_ANIMATION_IDLE = 0
    private const val VAMPY_ANIMATION_WALKING = 1
    private const val VAMPY_ANIMATION_WALKING_LEFT = 2
    private const val VAMPY_ANIMATION_WALKING_RIGHT = 3
    private const val VAMPY_ANIMATION_WALKING_BACKWARD = 4
    private const val VAMPY_ANIMATION_RUNNING = 5
    private const val VAMPY_ANIMATION_WAVING = 6
    private const val VAMPY_ANIMATION_DANCING = 7

    private var thirdPerson = false
    private var perspectiveTransition = 0f

    private var waving = false
    private var wavingTransition = 0f

    private val controls = inputs(
        bindKey(Key.ESC, setOf(ModifierKey.SHIFT)) { window.close() },
        bindKey(Key.W, on = InputTriggerEdge.PRESSED) { vampyPosDelta.z += 1 },
        bindKey(Key.S, on = InputTriggerEdge.PRESSED) { vampyPosDelta.z -= 1 },
        bindKey(Key.A, on = InputTriggerEdge.PRESSED) { vampyPosDelta.x += 1 },
        bindKey(Key.D, on = InputTriggerEdge.PRESSED) { vampyPosDelta.x -= 1 },
        bindKey(Key.Q) {
            waving = !waving
            logger.info { "Vampy ${if (waving) "started" else "stopped"} waving" }
        },
        bindKey(Key.E) {
            thirdPerson = !thirdPerson
            logger.info { "View set to ${if (thirdPerson) "3rd" else "1st"} person" }
        },
        bindKey(Key.R) {
            boneSelector = ++boneSelector % 5
            vampyShader.showBoneSelector = boneSelector
        },
        bindKey(Key.F) {
            showBoneWeights = !showBoneWeights
            vampyShader.showBoneWeights = showBoneWeights
        }
    )

    var walkingFactor = 0f
    var runningFactor = 0f

    val currentPosition = Vec3()
    val lastPosition = Vec3()
    val velocity = Vec3()
    val acceleration = Vec3()

    var lastDelta = 0.0

    var forwardFactor = 0f
    var leftFactor = 0f
    var rightFactor = 0f
    var backFactor = 0f

    //Vampy head bind position for first person camera snapping
    val vampyHeadNode = vampy.rootNode.find("mixamorig_Head")!!
    val vampyHeadBindPosition: Vec3c = vampyHeadNode
            .getGlobalTransform(1) //ignore scene root scaling
            .position

    init {
        entitySystem.addServices(RenderService(window.screenBuffer, renderer, camera, window.size, skybox = skybox))

        entitySystem.createEntity {
            +vampyTransform
            +Drawable(vampy, vampyShader)
        }

        entitySystem.createEntity {
            +cubeTransform
            +Drawable(cube, shader)
        }
    }

    override fun loop(delta: Double) {
        (window.scene as? DebugOverlay)?.cpuTime = avgCpuTime

        val running = controls.isPressed(Key.W) && controls.isPressed(Key.SHIFT)
                && !controls.isPressed(Key.A) && !controls.isPressed(Key.S)
                && !controls.isPressed(Key.D)

        if (!vampyPosDelta.equals(0f, 0f, 0f)) {
            acceleration(vampyPosDelta)
            acceleration.y = 0f
            acceleration.rotate(y = camera.yaw.deg)
            acceleration.normalize()
            acceleration(acceleration * if (running) 18f else 9f)
        } else {
            acceleration(0f)
        }

        //time-corrected verlet with second order taylor derivation + universal friction: p_n+1 = p_n + (1 - f) * (p_n - p_n-1) * (dt_n / dt_n-1) + a_n * dt_n * ((dt_n + dt_n-1) / 2)
        //and it still stutters quite regularly. and this, kids, is why we run physics as a fixed-interval coroutine, or in a separate thread
        val friction = .075f
        currentPosition(vampyTransform.position)
        val correctedDelta = (delta / lastDelta).toFloat()
        vampyTransform.position += (
                (
                        (velocity(vampyTransform.position) - lastPosition)
                        * if (correctedDelta.isFinite()) correctedDelta else 1f
                        * 1f - friction
                ) + (acceleration * (delta * (delta + lastDelta) / 2f))
        )
        lastPosition(currentPosition)
        vampyPosDelta(0f)

        vampyTransform.rotation().eulerAnglesYXZ = Vec3(0f, camera.yaw.deg, 0f)

        var diff = if (!running) 1f - walkingFactor else -walkingFactor
        diff *= delta.toFloat() * 5f
        walkingFactor += diff

        diff = if (running) 1f - runningFactor else -runningFactor
        diff *= delta.toFloat() * 5f
        runningFactor += diff

        diff = if (controls.isPressed(Key.W)) 1f - forwardFactor else -forwardFactor
        diff *= delta.toFloat() * 5f
        forwardFactor += diff
        diff = if (controls.isPressed(Key.A)) 1f - leftFactor else -leftFactor
        diff *= delta.toFloat() * 5f
        leftFactor += diff
        diff = if (controls.isPressed(Key.D)) 1 - rightFactor else -rightFactor;
        diff *= delta.toFloat() * 5f
        rightFactor += diff
        diff = if (controls.isPressed(Key.S)) 1 - backFactor else -backFactor
        diff *= delta.toFloat() * 5f
        backFactor += diff

        val factorSum = forwardFactor + leftFactor + rightFactor + backFactor
        if (factorSum != 0f) {
            forwardFactor /= factorSum
            leftFactor /= factorSum
            rightFactor /= factorSum
            backFactor /= factorSum
        }

        diff = if (waving) 1f - wavingTransition else -wavingTransition
        diff *= delta.toFloat() * 5f
        wavingTransition += diff

        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_IDLE] = max(1f - walkingFactor - runningFactor, 0f)
        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_WALKING] = forwardFactor * walkingFactor
        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_WALKING_LEFT] = leftFactor * walkingFactor
        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_WALKING_RIGHT] = rightFactor * walkingFactor
        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_WALKING_BACKWARD] = backFactor * walkingFactor
        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_RUNNING] = runningFactor
        vampyAnimator.animationMixer.weights[VAMPY_ANIMATION_WAVING] = wavingTransition
        vampyAnimator.update(delta)

        vampyShader.boneMatrices = vampyAnimator.transformMatricesArray
        vampyShader.globalLights = arrayOf(globalLight)

        shader.globalLights = arrayOf(globalLight)

        diff = if (thirdPerson) 1f - perspectiveTransition else -perspectiveTransition
        perspectiveTransition += (diff * delta * 10f).toFloat()
        var orbitPosition: Vec3? = null
        var worldUp: Vec3? = null
        //Only do calculations for both perspectives if currently switching
        if (perspectiveTransition < 1) {
            val headTransform = vampyAnimator.transforms[vampyHeadNode.bone!!.id]
            val headAnimPosition = headTransform.matrix * vampyHeadBindPosition
            headAnimPosition(headAnimPosition * vampyTransform.scale)
            val headAnimWorldPosition = vampyTransform.matrix * headAnimPosition

            orbitPosition = headAnimWorldPosition + Vec3(0f, .22f, 0f)
            worldUp = vampyTransform.rotation * (headTransform.rotation * Vec3.UP)
        }
        if (perspectiveTransition > 0) {
            val tpOrbitPosition = vampyTransform.position + Vec3.UP - (camera.direction * 1.75f)
            val tpWorldUp = Vec3(0f, 1f, 0f)

            if (orbitPosition != null) orbitPosition.lerp(tpOrbitPosition, perspectiveTransition)
            else orbitPosition = tpOrbitPosition
            if (worldUp != null) worldUp.lerp(tpWorldUp, perspectiveTransition)
            else worldUp = tpWorldUp
        }
        camera.position = orbitPosition!!
        camera.worldUp = worldUp!!

        vampyShader.combined = camera.combined
        vampyShader.viewPosition = camera.position

//            vampyBB.getTransform().set(vampy.getTransform());
//            renderer.renderWireframe(vampyBB, vampyShader, camera.getCombined());
//            renderer.renderBox(vampy.getTransform().getPosition(), vampy.getBoundingBox().getSize().mul(.01f), vampyShader, camera.getCombined());
    }

}

fun Node.find(name: String): Node? {
    if (this.name == name) return this
    for (child in children) {
        val match = child.find(name)
        if (match != null) return child
    }
    return null
}

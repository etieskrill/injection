package org.etieskrill.game.horde3d

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.service.impl.AnimationService
import org.etieskrill.engine.entity.service.impl.BoundingBoxService
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService
import org.etieskrill.engine.entity.service.impl.ParticleUpdateService
import org.etieskrill.engine.entity.service.impl.PhysicsService
import org.etieskrill.engine.entity.service.impl.PointShadowMappingService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.entity.service.impl.SnippetsService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShader
import org.etieskrill.engine.input.Input
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCharacterTranslationController
import org.etieskrill.engine.util.Loaders
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.Window.WindowSize
import org.joml.Math.toRadians
import org.joml.Vector3f
import org.joml.Vector4i
import org.joml.div
import org.joml.plus
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin

fun main() = EntityApplication.run()

val MODELS = Loaders.ModelLoader.get()!!

private val logger = KotlinLogging.logger {}

private val lightOnAmbient = Vector3f(1f)
private val lightOnDiffuse = Vector3f(5f)
private val lightOnSpecular = Vector3f(5f)
private val lightOff = Vector3f(0f)

//FIXME physics service probably a lil fucked in terms of friction and allat
object EntityApplication : App(
    Window(
        title = "Horde3D",
        size = WindowSize.LARGEST_FIT,
        mode = Window.WindowMode.BORDERLESS,
        samples = 4u,
        vSync = true,
        refreshRate = 60u
    )
) {

    private val camera = PerspectiveCamera(window.size)

    private val renderService: RenderService
    private val secondaryRenderService: RenderService

    private val world: World

    private val player: PlayerEntity
    private lateinit var zombie: Zombie

    private var light = true
    private var hdrReinhardMapping = true
    private var hdrExposure = 1f

    private var debugInterface: DebugInterface

    private var previousTime = 0.0

    init {
        GLUtils.addDebugLogging()

        renderer.queryGpuTime = false

        world = World(entitySystem)

        renderService = RenderService(window.screenBuffer, renderer, camera, window.size)
        secondaryRenderService = RenderService(
            window.screenBuffer, renderer,
            PerspectiveCamera(window.size).apply {
                position = Vector3f(-10f, 10f, -10f)
                setRotation(-45f, -45f, 0f)
                zoom = 10f
            },
            window.size / 4f
        ).apply {
            cullingCamera(camera)
            blur(false)
            customViewport(
                Vector4i(
                    (window.size.x() * 0.75f).toInt(), (window.size.y() * 0.75f).toInt(),
                    (window.size.x() * 0.25f).toInt(), (window.size.y() * 0.25f).toInt()
                )
            )
        }

        entitySystem.addServices(
            BoundingBoxService(),
            DirectionalShadowMappingService(renderer),
            PointShadowMappingService(renderer, DepthCubeMapArrayShader()), // DepthCubeMapArrayAnimatedShader()
            AnimationService(),
            ParticleUpdateService(),

            PhysicsService(PhysicsService.NarrowCollisionSolver.AABB_SOLVER),
            SnippetsService(),

            renderService,
            secondaryRenderService,
        )

        player = entitySystem.constructEntity { id -> PlayerEntity(id) }

        for (i in 0..<10) {
            val angle = toRadians((360f * i) / 10f)

            val zombie = entitySystem.constructEntity { Zombie(it) }
            zombie.transform.position.add(Vector3f(20f * cos(angle), 0f, 20f * sin(angle)))
            zombie.collider.previousPosition.set(zombie.transform.position)

            if (i == 9) this.zombie = zombie
        }

        window.cursorInputs += CursorCameraController(camera)
        window.keyInputs += KeyCharacterTranslationController(player.moveForce.force, camera)
            .removeBindings(Keys.SPACE.input, Keys.SHIFT.input)
            .addBindings(Input.bind(Keys.SPACE).toSimpleAction { player.dashState.trigger() })
        window.keyInputs += Input.of(
            Input.bind(Keys.Q).toSimpleAction {
                light = !light
                logger.info { "Turning sunlight ${if (light) "on" else "off"}" }

                world.sunLight.apply {
                    setAmbient(if (light) lightOnAmbient else lightOff)
                    setDiffuse(if (light) lightOnDiffuse else lightOff)
                    setSpecular(if (light) lightOnSpecular else lightOff)
                }
            },
            Input.bind(Keys.E).toSimpleAction {
                hdrReinhardMapping = !hdrReinhardMapping
                renderService.hdrShader.reinhard = hdrReinhardMapping
            },
            Input.bind(Keys.T).toSimpleAction {
                hdrExposure += 0.25f
                renderService.hdrShader.exposure = hdrExposure
            },
            Input.bind(Keys.G).toSimpleAction {
                hdrExposure -= 0.25f
                renderService.hdrShader.exposure = hdrExposure
            },
            Input.bind(Keys.F1).toSimpleAction {
                renderService.boundingBoxRenderService.toggleRenderBoundingBoxes()
            }
        )

        window.cursor.disable()

        //FIXME loading the scene (the label font specifically) before the above stuff causes a segfault from freetype??
        debugInterface = DebugInterface(window.screenBuffer, window.size, renderer, pacer)
        window.scene = debugInterface

        GLUtils.removeDebugLogging()
    }

    override fun loop(delta: Double) {
//        player.getTransform().getPosition().sub(zombie.getTransform().getPosition(), zombie.getAcceleration().getForce());
//        zombie.getAcceleration().getForce().normalize().mul(.015f * zombie.getAcceleration().getFactor());

        camera.setRotation(-45f, -45f, 0f)
        camera.setPosition(
            Vector3f(player.transform.position)
                .add(0f, 2f, 0f)
                .sub(Vector3f(camera.direction).mul(8f))
        )

        world.sunTransform.position = camera.position + Vector3f(50f)
        world.cubeTransform.rotation.rotateAxis(delta.toFloat(), 1f, 1f, 1f)

        if (floor(pacer.timerTimeSeconds) != floor(previousTime)) {
            logger.info {
                "Fps: ${"%4.1f".format(pacer.averageFPS)}, cpu time: ${
                    "%5.2f".format(avgCpuTime)
                }ms, gpu time: ${
                    "%5.2f".format(renderer.averagedGpuTime / 1_000_000.0)
                }, gpu delay: ${"%5.2f".format(renderer.gpuDelay / 1_000_000.0)}ms"
            }
        }
        previousTime = pacer.timerTimeSeconds
        debugInterface.fpsLabel.text =
            "Fps: %f\nRender calls: %d\nTriangles: %d\nMapping: %s\nExposure: %4.2f\nDash cooldown: %.0f".format(
                round(pacer.averageFPS), renderer.renderCalls, renderer.trianglesDrawn,
                if (hdrReinhardMapping) "Reinhard" else "Exposure", hdrExposure, max(0f, player.dashState.cooldown)
            )
    }

}

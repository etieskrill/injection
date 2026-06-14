package org.etieskrill.game.horde3d

import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.AnimationService
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.animation.Animator
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap
import org.etieskrill.engine.graphics.gl.shader.impl.AnimationShader
import org.etieskrill.engine.graphics.gl.shader.impl.PhongNoMaterialShader
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.model.ModelFactory
import org.etieskrill.engine.graphics.model.loader.Loader
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.util.EngineAnimationLoader
import org.etieskrill.engine.util.EngineModelLoader
import org.etieskrill.engine.window.Window
import org.joml.Math.toRadians
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun main() = RuntimeMeshOptimisation.run()

object RuntimeMeshOptimisation : App(
    Window(
        size = Window.WindowSize.LARGEST_FIT,
        mode = Window.WindowMode.BORDERLESS,
        vSync = true
    )
) {

    val zombieTransforms = mutableListOf<Transform>()

    init {
        val camera = PerspectiveCamera(window.size).setRotation(0f, -90f, 0f)

        entitySystem.addServices(
            AnimationService(),
            DirectionalShadowMappingService(renderer),
            RenderService(window.screenBuffer, renderer, camera, window.size)
        )

        entitySystem.createEntity {
            +Transform(position = Vector3f(0f, -1.5f, 0f))
            +Drawable(ModelFactory.box(Vector3f(30f, 1f, 30f)), PhongNoMaterialShader())
        }

        entitySystem.createEntity {
            +DirectionalLightComponent(
                DirectionalLight(Vector3f(1f, -1f, -1f)),
                DirectionalShadowMap(Vector2i(4096)),
                OrthographicCamera(Vector2i(4096), 20f, -20f, -20f, 20f).apply {
                    position = Vector3f(10f)
                    setRotation(-45f, 215f, 0f)
                    far = 40f
                }
            )
        }

        spawnZombies()
        spawnSkeletonZombies()

        window.keyInputs += KeyCameraController(camera)
        window.cursorInputs += CursorCameraController(camera)
        window.cursor.disable()

        window.scene = DebugInterface(window.screenBuffer, window.size, renderer, pacer)
    }

    private fun spawnZombies() {
        val numZombies = 100
        val random = Random(69420)

        for (i in 0..numZombies) {
            val angle = toRadians(360f * i / numZombies)

            val model = EngineModelLoader.load("zombie") {
                Model.Builder("mixamo_zombie_skinned_walking.glb")
                    .optimiseMeshes()
                    .build()
            }

            val animator = Animator(model).add(EngineAnimationLoader.load("mixamo_zombie_walking") {
                Loader.loadModelAnimations("mixamo_zombie_walking.glb", model)[0]
            })
            animator.play(random.nextDouble() * 0.25)

            val transform = Transform(position = Vector3f(8f * cos(angle), -1f, 8f * sin(angle)))
            zombieTransforms += transform

            entitySystem.createEntity {
                +transform
                +Drawable(model, ZombieShader())
                +animator
            }
        }
    }

    private fun spawnSkeletonZombies() {
        val numSkellyZombies = 25
        val random = Random(69420)

        for (i in 0..numSkellyZombies) {
            val angle = toRadians(360f * i / numSkellyZombies)

            //FIXME why the skellington rig borked?
            val model = EngineModelLoader.load("skeleton_zombie") {
                Model.Builder("mixamo_skeletonzombie_skin.glb")
                    .optimiseMeshes()
                    .build()
            }

            val animator = Animator(model).add(EngineAnimationLoader.load("silly_dancing") {
                Loader.loadModelAnimations("mixamo_bboy_hip_hop.fbx", model) { modelBone, animBone ->
                    modelBone.substring(modelBone.lastIndexOf(':')) in animBone
                }[0]
            }) { layer -> layer.playbackSpeed = 0.8 }
            animator.play(random.nextDouble() * 0.175)

            entitySystem.createEntity {
                +Transform(position = Vector3f(8f * cos(angle), 1f, 8f * sin(angle)))
                +Drawable(model, AnimationShader())
                +animator
            }
        }
    }

    override fun loop(delta: Double) {
        zombieTransforms.forEachIndexed { i, transform ->
            val angle = toRadians(360f * i / zombieTransforms.size)
            val anglePosition = angle + ((0.05f * pacer.timerTimeSeconds) % (2f * PI)).toFloat()

            transform.position = Vector3f(9f * cos(anglePosition), -1f, 8f * sin(anglePosition))
            transform.rotation.rotationY(-anglePosition)
        }
    }

}

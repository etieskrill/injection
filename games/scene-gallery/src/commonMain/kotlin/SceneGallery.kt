package net.bifreus.games.scene.gallery

import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.DeferredRenderService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.model.PhongMaterial
import org.etieskrill.engine.graphics.model.loader.loadModel
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.plane
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.shader.impl.StaticShader
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.input.controller.KeyCharacterController
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.WindowSize
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.div
import org.joml.plus
import org.joml.times
import org.joml.timesAssign

fun main() = SceneGallery.run()

//TODO add window align function
object SceneGallery : App(Window(WindowSize.FHD)) {

    val camera = PerspectiveCamera(window.size).apply {
        position = Vector3f(0f, 2f, 0f)
    }

    init {
        //region Entities
        entitySystem.createEntity {
            +Transform()
            +Drawable(model("plane") {
                plane(
                    Vector2f(-100f), Vector2f(100f), material = PhongMaterial(
                        textureScale = 80f,
                        diffuseTexture = Texture2D(
                            Vector2i(4096), TextureType.DIFFUSE, format = TextureFormat.SRGB,
                            file = "materials/concrete_rock_path/concrete_rock_path_diff_4k.jpg",
                        ),
                        normalTexture = Texture2D(
                            Vector2i(4096), TextureType.NORMAL, format = TextureFormat.RGB,
                            file = "materials/concrete_rock_path/concrete_rock_path_nor_gl_4k.jpg"
                        ),
                        specularTexture = Texture2D(
                            Vector2i(4096), TextureType.ROUGHNESS, format = TextureFormat.GRAY,
                            file = "materials/concrete_rock_path/concrete_rock_path_rough_4k.jpg"
                        )
                    )
                )
            })
        }

        entitySystem.createEntity {
            +Transform(position = Vector3f(10f, 0f, 0f))
            +Drawable(loadModel("models/dark_spike.glb"))
        }
        //endregion

        entitySystem.addServices(
            DeferredRenderService(
                renderer, window.screenBuffer, camera,
                DeferredDistanceFogPostShader()
            )
        )

        window.cursorInputs += CursorCameraController(camera)
        window.cursor.disable()

        window.keyInputs += KeyCharacterController(camera, { delta, target, deltaPosition, speed ->
            deltaPosition.set(target.relativeTranslation(deltaPosition))
            deltaPosition.y = 0f
            //TODO take up -0 into the NaN normalise bullshit territory too
            if (!deltaPosition.equals(VEC3_ZERO, 0.00001f)) deltaPosition.normalize()
            deltaPosition *= speed * delta.toFloat()
            target.position += deltaPosition
        }, 3f)
    }

    override fun loop(delta: Double) = Unit

}

val VEC3_ZERO: Vector3fc = Vector3f(0f, 0f, 0f)

package net.bifreus.games.scene.gallery

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec3
import net.bifreus.kml.deg
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.DeferredRenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.model.PhongMaterial
import org.etieskrill.engine.graphics.model.loader.loadModel
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.plane
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.input.InputTriggerEdge
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.bindKey
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCharacterController
import org.etieskrill.engine.input.inputs
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.WindowSize

fun main() = SceneGallery.run()

//TODO add window align function
object SceneGallery : App(Window(WindowSize.FHD)) {

    val camera = PerspectiveCamera(window.size).apply {
        position = Vec3(0f, 2f, 0f)
    }

    init {
        //region Entities
        entitySystem.createEntity {
            +Transform()
            +Drawable(model("plane") {
                plane(
                    Vec2(-100f), Vec2(100f), material = PhongMaterial(
                        textureScale = 80f,
                        diffuseTexture = Texture2D(
                            Vec2i(4096), TextureType.DIFFUSE, format = TextureFormat.SRGB,
                            file = "materials/concrete_rock_path/concrete_rock_path_diff_4k.jpg",
                        ),
                        normalTexture = Texture2D(
                            Vec2i(4096), TextureType.NORMAL, format = TextureFormat.RGB,
                            file = "materials/concrete_rock_path/concrete_rock_path_nor_gl_4k.jpg"
                        ),
                        specularTexture = Texture2D(
                            Vec2i(4096), TextureType.ROUGHNESS, format = TextureFormat.GRAY,
                            file = "materials/concrete_rock_path/concrete_rock_path_rough_4k.jpg"
                        )
                    )
                )
            })
        }

        entitySystem.createEntity {
            +Transform(position = Vec3(10f, 0f, 0f))
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
            deltaPosition(target.relativeTranslation(deltaPosition))
            deltaPosition.y = 0f
            //TODO take up -0 into the NaN normalise bullshit territory too
            if (deltaPosition != Vec3.ZERO) deltaPosition.normalize()
            deltaPosition(deltaPosition * (speed * delta.toFloat()))
            target.position += deltaPosition
        }, 3f)
    }

    override fun loop(delta: Double) = Unit

}

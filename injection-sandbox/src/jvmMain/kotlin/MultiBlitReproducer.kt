import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.container.VBox
import org.etieskrill.engine.scene.element.Image
import org.joml.Vector2f

fun main() = MultiBlitReproducer.run()

object MultiBlitReproducer : App() {

    init {
        window.scene = Scene(
            Batch(window.screenBuffer, renderer, textRenderer),
            VBox(Image("textures/icons/chevron-down-solid-black.png").apply { size = Vector2f(50f) }),
            OrthographicCamera(window.size)
        )

        entitySystem.addServices(
            RenderService(
                window.screenBuffer,
                renderer,
                PerspectiveCamera(window.size),
                window.size
            )
        )
    }

    override fun loop(delta: Double) {}

}

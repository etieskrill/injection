import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.TextField

fun main() {
    Noteify.run()
}

object Noteify : GameApplication() {

    init {
        window.scene = Scene(
            Batch(renderer, window.currentSize),
            TextField(),
            OrthographicCamera(window.currentSize)
        )
    }

    override fun loop(delta: Double) {}

}

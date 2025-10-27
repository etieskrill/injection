import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.TextField
import org.etieskrill.engine.window.window

fun main() {
    Noteify.run()
}

object Noteify : App(window {
    resizeable = true
}) {

    init {
        window.scene = Scene(
            Batch(renderer, window.currentSize),
            TextField(),
            OrthographicCamera(window.currentSize)
        )
    }

    override fun loop(delta: Double) {}

}

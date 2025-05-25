import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.Container
import org.etieskrill.engine.scene.component.Node
import org.etieskrill.engine.scene.component.TextField
import org.etieskrill.engine.scene.component.WidgetContainer

fun main() {
    TextFieldApp().run()
}

class TextFieldApp : GameApplication() {
    init {
        val camera = OrthographicCamera(window.currentSize)
        window.scene = Scene(
            Batch(renderer, window.currentSize),
            Container(
                WidgetContainer(TextField()).apply {
                    alignment = Node.Alignment.FIXED_POSITION
                }
            ),
            camera
        )
    }

    override fun loop(delta: Double) {}
}
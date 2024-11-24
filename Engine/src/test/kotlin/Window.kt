import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import kotlin.test.Test

class Window {

    val window: Window = window {}

    @Test
    fun shouldStart() {
    }

    @Test
    fun shouldLoadModel() {
        val model = Model.ofFile("backpack.obj")
    }

}
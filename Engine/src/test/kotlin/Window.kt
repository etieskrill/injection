import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import kotlin.test.Test
import kotlin.test.assertNotNull

class Window {

    val window: Window = window {}

    @Test
    fun shouldStart() {
        assertNotNull(window)
    }

    @Test
    fun shouldLoadModel() {
        assertNotNull(Model.ofFile("backpack.obj"))
    }

}
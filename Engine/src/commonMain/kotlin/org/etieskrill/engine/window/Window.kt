package org.etieskrill.engine.window

import kotlinx.coroutines.CoroutineScope
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.window.WindowMode.WINDOWED
import org.etieskrill.engine.window.WindowSize.DEFAULT

expect class Window : Disposable {

    constructor(
        size: Vec2ic = DEFAULT,
        mode: WindowMode = WINDOWED,
        title: String = "Injection Window",
        refreshRate: UInt? = null,
        position: Vec2ic? = null,
        cursor: Cursor = Cursor(),
        resizeable: Boolean = false,
        vSync: Boolean = false,
        samples: UInt = 4u,
        createHidden: Boolean = false,
        transparency: Boolean = false
    )

    var size: Vec2ic
    val aspectRatio: Float
    var resizeable: Boolean

    var mode: WindowMode

    var visible: Boolean

    var isClosing: Boolean
    fun close()

    var refreshRate: UInt

    var title: String

    var cursor: Cursor

    var position: Vec2ic

    val keyInputs: MutableList<KeyInputHandler>
    val cursorInputs: MutableList<CursorInputHandler>

    val screenBuffer: FrameBuffer

    var uiScope: CoroutineScope

    var scene: Scene?

    var graphicsContext: GraphicsContext

    fun update(delta: Double)

}

object WindowSize {
    val HD: Vec2ic = Vec2i(1280, 720)
    val FHD: Vec2ic = Vec2i(1920, 1080)
    val WUXGA: Vec2ic = Vec2i(1920, 1200)
    val WQHD: Vec2ic = Vec2i(2560, 1440)
    val UHD: Vec2ic = Vec2i(3840, 2160)
    val VGA: Vec2ic = Vec2i(640, 480)
    val SVGA: Vec2ic = Vec2i(800, 600)
    val XGA: Vec2ic = Vec2i(1024, 768)
    val UWHD: Vec2ic = Vec2i(2560, 1080)
    val UWQHD: Vec2ic = Vec2i(3440, 1440)
    val UHD_4K: Vec2ic = Vec2i(3840, 2160)
    val THICC: Vec2ic = Vec2i(4000, 100)

    val LARGEST_FIT: Vec2ic = Vec2i(-1, -1) //not in values on purpose (sentinel value)
    val DEFAULT: Vec2ic = Vec2i(-2, -2) //not in values on purpose (sentinel value)

    /**
     * Finds the largest standard resolution that fits into ([width], [height]). Maximises width over height.
     *
     * @param width the maximum width
     * @param height the maximum height
     * @return the largest resolution <= ([width], [height]), or `null` if no format is small enough
     */
    fun getLargestFit(width: Int, height: Int): Vec2ic = values
        .filter { width >= it.x && height >= it.y }
        .fold(DEFAULT) { current, new ->
            new.takeIf { current.x < new.x || (current.x <= new.x && current.y < new.y) } ?: current
        }

    private val values = arrayOf(
        HD, FHD, WUXGA, WQHD, UHD, VGA, SVGA, XGA, UWHD, UWQHD, UHD_4K, THICC
    )
}

enum class WindowMode { FULLSCREEN, BORDERLESS, WINDOWED }

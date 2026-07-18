package org.etieskrill.engine.window

import kotlinx.coroutines.CoroutineScope
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.window.WindowMode.WINDOWED
import org.etieskrill.engine.window.WindowSize.DEFAULT
import org.joml.Vector2i
import org.joml.Vector2ic

expect class Window : Disposable {

    constructor(
        size: Vector2ic = DEFAULT,
        mode: WindowMode = WINDOWED,
        title: String = "Injection Window",
        refreshRate: UInt? = null,
        position: Vector2ic? = null,
        cursor: Cursor = Cursor(),
        resizeable: Boolean = false,
        vSync: Boolean = false,
        samples: UInt = 4u,
        createHidden: Boolean = false,
        transparency: Boolean = false
    )

    var size: Vector2ic
    val aspectRatio: Float
    var resizeable: Boolean

    var mode: WindowMode

    var visible: Boolean

    var isClosing: Boolean
    fun close()

    var refreshRate: UInt

    var title: String

    var cursor: Cursor

    var position: Vector2ic

    val keyInputs: MutableList<KeyInputHandler>
    val cursorInputs: MutableList<CursorInputHandler>

    val screenBuffer: FrameBuffer

    var uiScope: CoroutineScope

    var scene: Scene?

    var graphicsContext: GraphicsContext

    fun update(delta: Double)

}

object WindowSize {
    val HD: Vector2ic = Vector2i(1280, 720)
    val FHD: Vector2ic = Vector2i(1920, 1080)
    val WUXGA: Vector2ic = Vector2i(1920, 1200)
    val WQHD: Vector2ic = Vector2i(2560, 1440)
    val UHD: Vector2ic = Vector2i(3840, 2160)
    val VGA: Vector2ic = Vector2i(640, 480)
    val SVGA: Vector2ic = Vector2i(800, 600)
    val XGA: Vector2ic = Vector2i(1024, 768)
    val UWHD: Vector2ic = Vector2i(2560, 1080)
    val UWQHD: Vector2ic = Vector2i(3440, 1440)
    val UHD_4K: Vector2ic = Vector2i(3840, 2160)
    val THICC: Vector2ic = Vector2i(4000, 100)

    val LARGEST_FIT: Vector2ic = Vector2i(-1, -1) //not in values on purpose (sentinel value)
    val DEFAULT: Vector2ic = Vector2i(-2, -2) //not in values on purpose (sentinel value)

    /**
     * Finds the largest standard resolution that fits into ([width], [height]). Maximises width over height.
     *
     * @param width the maximum width
     * @param height the maximum height
     * @return the largest resolution <= ([width], [height]), or `null` if no format is small enough
     */
    fun getLargestFit(width: Int, height: Int): Vector2ic? = values
        .filter { width >= it.x() && height >= it.y() }
        .fold(DEFAULT) { current, new ->
            new.takeIf { current.x() < new.x() || (current.x() <= new.x() && current.y() < new.y()) } ?: current
        }

    private val values = arrayOf(
        HD, FHD, WUXGA, WQHD, UHD, VGA, SVGA, XGA, UWHD, UWQHD, UHD_4K, THICC
    )
}

enum class WindowMode { FULLSCREEN, BORDERLESS, WINDOWED }

package org.etieskrill.engine.window;

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.config.GLContextConfig
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.framebuffer.ScreenBuffer
import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.window.Window.GLFWError.entries
import org.etieskrill.engine.window.Window.WindowMode.*
import org.etieskrill.engine.window.Window.WindowSize.DEFAULT
import org.etieskrill.engine.window.Window.WindowSize.LARGEST_FIT
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL13C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL20C.GL_MAX_TEXTURE_IMAGE_UNITS
import org.lwjgl.opengl.GL20C.GL_SHADING_LANGUAGE_VERSION
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.system.Platform
import kotlin.properties.Delegates.notNull
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias GLFWId = Long

private val logger = KotlinLogging.logger {}

class Window(
    size: Vector2ic = DEFAULT,
    mode: WindowMode = WINDOWED,
    title: String = "Injection Window",
    refreshRate: UInt? = null,
    position: Vector2ic? = null,
    cursor: Cursor = Cursor(),
    resizeable: Boolean = false,
    private val vSync: Boolean = false,
    private val samples: UInt = 4u,
    private val createHidden: Boolean = false,
    private val transparency: Boolean = false
) : Disposable {

    var size: Vector2ic by object : ReadWriteProperty<Window, Vector2ic> {
        private var field: Vector2ic? = null

        override fun getValue(thisRef: Window, property: KProperty<*>) = checkNotNull(field)

        override fun setValue(thisRef: Window, property: KProperty<*>, value: Vector2ic) {
            if (field == null) {
                field = value
                return
            }

            check(resizeable) { "Window is not resizeable" }
            glfwSetWindowSize(id, value.x(), value.y())
            //TODO resize callbacks (update screenBuffer too!)
            (field as Vector2i).set(value)
        }
    }

    val aspectRatio: Float get() = size.x().toFloat() / size.y().toFloat()

    var resizeable: Boolean = resizeable
        set(value) {
            glfwSetWindowAttrib(id, GLFW_RESIZABLE, if (value) GLFW_TRUE else GLFW_FALSE)
            field = value
        }

    var mode: WindowMode = mode
        set(value) {
            //TODO glfwSetWindowMonitor
            glfwSetWindowAttrib(
                id, GLFW_DECORATED, when (value) {
                    FULLSCREEN, BORDERLESS -> GLFW_FALSE
                    WINDOWED -> GLFW_TRUE
                }
            )
            field = value
        }

    var visible: Boolean = true
        set(value) {
            if (value) glfwShowWindow(id)
            else glfwHideWindow(id)
            field = value
        }

    var isClosing: Boolean
        get() = glfwWindowShouldClose(id)
        set(value) = glfwSetWindowShouldClose(id, value)

    fun close() {
        isClosing = true
    }

    var refreshRate: UInt by object : ReadWriteProperty<Window, UInt> {
        private var field: UInt? = null

        override fun getValue(thisRef: Window, property: KProperty<*>) = checkNotNull(field)

        override fun setValue(thisRef: Window, property: KProperty<*>, value: UInt) {
            if (field == null) {
                field = value
                return
            }

            check(value > 0u) { "Refresh rate must be greater than zero" }
            check(mode == FULLSCREEN) { "Window must be fullscreen mode to change refresh rate after creation" }
            TODO("glfwSetWindowMonitor")
            field = value
        }
    }

    var title: String = title
        set(value) {
            glfwSetWindowTitle(id, value)
            field = value
        }

    var cursor: Cursor = cursor
        set(value) {
            field.window = null
            value.window = this
            field = value
        }

    private val xPosBuffer = IntArray(1)
    private val yPosBuffer = IntArray(1)
    private val posBuffer = Vector2i()

    var position: Vector2ic
        get() {
            glfwGetWindowPos(id, xPosBuffer, yPosBuffer)
            return posBuffer.apply { x = xPosBuffer[0]; y = yPosBuffer[0] }
        }
        //on Windows, will only succeed if entire window is still within screen space after translation
        set(value) {
            glfwSetWindowSize(id, value.x(), value.y())
            checkError("If you are not on Wayland, be concerned")
        }

    init {
        check(glfwInit()) { "Unable to initialize glfw library" }

        refreshRate?.let { check(it > 0u) { "Refresh rate must be greater than zero" } }
    }

    val keyInputs: MutableList<KeyInputHandler> = mutableListOf()
    val cursorInputs: MutableList<CursorInputHandler> = mutableListOf()

    private val internalScreenBuffer = ScreenBuffer(Vector2i(size))
    val screenBuffer: FrameBuffer get() = internalScreenBuffer

    lateinit var uiScope: CoroutineScope //*extremely loud alarm sound*

    var scene: Scene? = null
        set(value) {
            value?.size = size
            field = value
        }

    internal var id: GLFWId by notNull()

    //TODO provide methods to change primary monitor
    internal var monitor: GLFWId by notNull()

    companion object {
        const val MIN_GL_CONTEXT_MAJOR_VERSION = 3
        const val MIN_GL_CONTEXT_MINOR_VERSION = 3

        var USE_RAW_MOUSE_MOTION_IF_AVAILABLE = true
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

    init {
        clearError()
        init(size, refreshRate, position)
        checkErrorThrowing("Error during window initialisation")
        logger.info { "Created window with settings: $this" }
    }

    private fun init(size: Vector2ic, refreshRate: UInt?, position: Vector2ic?) {
        if (!glfwInit()) throw IllegalStateException("Unable to initialize glfw library")

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, MIN_GL_CONTEXT_MAJOR_VERSION)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, MIN_GL_CONTEXT_MINOR_VERSION)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        }

        //TODO more sophisticated & consumer-controlled monitor choice / general window configuration
        monitor = glfwGetPrimaryMonitor()
        check(monitor != 0L) { "Could not find primary monitor" }

        val videoMode = glfwGetVideoMode(monitor) ?: error("Video mode for monitor could not be retrieved")

        this.size = when (size) {
            LARGEST_FIT -> WindowSize.getLargestFit(videoMode.width(), videoMode.height())
                ?: error("No reasonable resolution could be found")

            DEFAULT -> Vector2i(videoMode.width() / 4, videoMode.height() / 2)
            else -> size
        }

        glfwWindowHint(GLFW_RESIZABLE, if (resizeable) GLFW_TRUE else GLFW_FALSE)
        glfwWindowHint(
            GLFW_DECORATED, when (mode) {
                FULLSCREEN, BORDERLESS -> GLFW_FALSE
                WINDOWED -> GLFW_TRUE
            }
        )

        this.refreshRate = refreshRate ?: videoMode.refreshRate().toUInt()
        glfwWindowHint(GLFW_REFRESH_RATE, this.refreshRate.toInt())
        glfwWindowHint(GLFW_SAMPLES, if (samples >= 0u) samples.toInt() else GLFW_DONT_CARE)
        glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, if (transparency) GLFW_TRUE else GLFW_FALSE)
        glfwWindowHint(GLFW_VISIBLE, if (createHidden) GLFW_FALSE else GLFW_TRUE)

        checkErrorThrowing("Error while setting up window configuration")

        id = glfwCreateWindow(
            this.size.x(), this.size.y(),
            title,
            when (mode) {
                FULLSCREEN -> monitor
                WINDOWED, BORDERLESS -> 0L
            },
            0L
        )
        checkErrorThrowing("Error during window creation")
        check(id != 0L) { "Could not create GLFW window" }

        initGl()
        checkErrorThrowing("Failed to initialise OpenGL context")

        glfwSwapInterval(if (vSync) 1 else 0)

        configInput()

        this.title = title //to call setter
        position?.let { this.position = it }
        cursor.window = this

        internalScreenBuffer.size = this.size

//        glfwSetErrorCallback(null);
    }

    private fun initGl() {
        glfwMakeContextCurrent(id)

        val caps = GL.createCapabilities()

        GLContextConfig.CONFIG.set(
            GLContextConfig(
                glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS)
            )
        )

        val glExtensions = glGetString(GL_EXTENSIONS)
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?: listOf<String>()
        logger.debug {
            """
                |   GL context:
                |       version: ${glGetString(GL_VERSION)}
                |       shading language version: ${glGetString(GL_SHADING_LANGUAGE_VERSION)}
                |       extensions (${glExtensions.size} total): ${glExtensions.joinToString(", ")}
                |       renderer: ${glGetString(GL_RENDERER)}
                |       vendor: ${glGetString(GL_VENDOR)}
            """.trimMargin()
        }

        if (samples > 0u) glEnable(GL_MULTISAMPLE)
        else glDisable(GL_MULTISAMPLE)
    }

    fun configInput() {
        glfwSetFramebufferSizeCallback(id) { _, width, height ->
            (size as Vector2i).set(width, height)

            internalScreenBuffer.size = size
            scene?.let { it.size = size }
        }
//        glfwSetWindowRefreshCallback() //TODO can be used to update during moving, resizing etc.

        if (USE_RAW_MOUSE_MOTION_IF_AVAILABLE && glfwRawMouseMotionSupported()) {
            glfwSetInputMode(id, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }

        glfwSetKeyCallback(id) { windowId, key, scancode, action, mods ->
            if (windowId != id) return@glfwSetKeyCallback

            keyInputs.forEach { it.invoke(Key.Type.KEYBOARD, key, action, mods) }
            scene?.invoke(Key.Type.KEYBOARD, key, action, mods)
        }
        glfwSetCharCallback(id) { windowId, scancode ->
            if (windowId != id) return@glfwSetCharCallback

            keyInputs.forEach { it.invokeCharacter(scancode.toChar()) }
            scene?.invokeCharacter(scancode.toChar())
        }
        val posX = DoubleArray(1)
        val posY = DoubleArray(1)
        glfwSetMouseButtonCallback(id) { window, button, action, glfwMods ->
            if (window != id) return@glfwSetMouseButtonCallback

            keyInputs.forEach { it.invoke(Key.Type.MOUSE, button, action, glfwMods) }

            val key = Keys.fromGlfw(button) ?: return@glfwSetMouseButtonCallback
            val keyWithMods = key.withMods(Keys.Mod.fromGlfw(glfwMods))
            val action = Keys.Action.fromGLFW(action)!!

            glfwGetCursorPos(id, posX, posY)

            cursorInputs.forEach { it.invokeClick(keyWithMods, action, posX[0], posY[0]) }
            scene?.invokeClick(keyWithMods, action, posX[0], posY[0])
        }
        glfwSetCursorPosCallback(id) { window, xpos, ypos ->
            if (window != id) return@glfwSetCursorPosCallback

            cursorInputs.forEach { it.invokeMove(xpos, ypos) }
            scene?.invokeMove(xpos, ypos)
        }
        glfwSetScrollCallback(id) { window, xoffset, yoffset ->
            if (window != id) return@glfwSetScrollCallback

            cursorInputs.forEach { it.invokeScroll(xoffset, yoffset) }
            scene?.invokeScroll(xoffset, yoffset)
        }
    }

    fun update(delta: Double) {
        scene?.run {
            setCoroutineScope(uiScope)
            size = this@Window.size
            update(delta)

            camera.position = Vector3f(this@Window.size, 0f) / 2f
            camera.viewportSize = this@Window.size
            render()
        }

        glfwSwapBuffers(id) //Buffers are usually swapped before polling events
        glfwPollEvents() //Also proves to system that window has not frozen

        keyInputs.filterIsInstance<KeyInputManager>().forEach { it.update(delta) }
    }

//    fun useLargestSizePossible() {
//        if (size == WindowSize.LARGEST_FIT) {
//            monitor = glfwGetPrimaryMonitor();
//            if (monitor == NULL) throw new IllegalStateException("Could not find primary monitor");
//
//            var videoMode = requireNonNull(glfwGetVideoMode(monitor), "Failed to get video mode");
//            size = WindowSize.getLargestFit(videoMode.width(), videoMode.height());
//        }
//    }

    private fun clearError() = glfwGetError(null)

    private fun checkError(header: String? = null) = MemoryStack.stackPush().use { stack ->
        val description = stack.mallocPointer(1)
        val error = GLFWError.fromGLFW(glfwGetError(description))
        if (error != GLFWError.NO_ERROR) {
            logger.warn { "${header ?: "GLFW error occurred"}: $error ${memUTF8(description.get())}" }
        }
    }

    private fun checkErrorThrowing(header: String? = null) = MemoryStack.stackPush().use { stack ->
        val description = stack.mallocPointer(1)
        val error = GLFWError.fromGLFW(glfwGetError(description))
        if (error != GLFWError.NO_ERROR) {
            throw IllegalStateException("${header ?: "GLFW error occurred"}\n $error ${memUTF8(description.get())}")
        }
    }

    enum class GLFWError(val glfw: Int) {
        NO_ERROR(GLFW_NO_ERROR),
        NOT_INITIALIZED(GLFW_NOT_INITIALIZED),
        NO_CURRENT_CONTEXT(GLFW_NO_CURRENT_CONTEXT),
        INVALID_ENUM(GLFW_INVALID_ENUM),
        INVALID_VALUE(GLFW_INVALID_VALUE),
        OUT_OF_MEMORY(GLFW_OUT_OF_MEMORY),
        API_UNAVAILABLE(GLFW_API_UNAVAILABLE),
        VERSION_UNAVAILABLE(GLFW_VERSION_UNAVAILABLE),
        PLATFORM_ERROR(GLFW_PLATFORM_ERROR),
        FORMAT_UNAVAILABLE(GLFW_FORMAT_UNAVAILABLE),
        NO_WINDOW_CONTEXT(GLFW_NO_WINDOW_CONTEXT),
        CURSOR_UNAVAILABLE(GLFW_CURSOR_UNAVAILABLE),
        FEATURE_UNAVAILABLE(GLFW_FEATURE_UNAVAILABLE),
        FEATURE_UNIMPLEMENTED(GLFW_FEATURE_UNIMPLEMENTED),
        PLATFORM_UNAVAILABLE(GLFW_PLATFORM_UNAVAILABLE);

        companion object {
            fun fromGLFW(glfwError: Int): GLFWError =
                entries.find { it.glfw == glfwError }
                    ?: throw IllegalStateException("Unknown GLFW error: 0x${Integer.toHexString(glfwError)}")
        }
    }

    @Deprecated(message = "Only call if you know what you are doing.")
    fun attachContext() {
        glfwMakeContextCurrent(id)
        GL.createCapabilities()
    }

    @Deprecated(message = "Only call if you know what you are doing.")
    fun detachContext() = glfwMakeContextCurrent(0L)

    override fun dispose() {
        glfwDestroyWindow(id)
        cursor.dispose()
    }

    @Override
    override fun toString(): String =
        "Window{mode=$mode, size=$size, targetFrameRate=$refreshRate, " +
                "vSyncEnabled=$vSync, samples=$samples, title='$title', scene=$scene}"

}

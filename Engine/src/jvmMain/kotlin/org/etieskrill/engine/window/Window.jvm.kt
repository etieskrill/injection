package org.etieskrill.engine.window;

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.ScreenBuffer
import org.etieskrill.engine.graphics.framebuffer.ScreenBufferInstance
import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.input.parseGlfwAction
import org.etieskrill.engine.input.parseGlfwKey
import org.etieskrill.engine.input.parseGlfwModifierKeys
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.window.WindowMode.*
import org.etieskrill.engine.window.WindowSize.DEFAULT
import org.etieskrill.engine.window.WindowSize.LARGEST_FIT
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL13C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL20C.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.system.Platform
import kotlin.properties.Delegates.notNull
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias GLFWId = Long

private val logger = KotlinLogging.logger {}

actual class Window actual constructor(
    size: Vector2ic,
    mode: WindowMode,
    title: String,
    refreshRate: UInt?,
    position: Vector2ic?,
    cursor: Cursor,
    resizeable: Boolean,
    private val vSync: Boolean,
    private val samples: UInt,
    private val createHidden: Boolean,
    private val transparency: Boolean
) : Disposable {

    actual var size: Vector2ic by object : ReadWriteProperty<Window, Vector2ic> {
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

    actual val aspectRatio: Float get() = size.x().toFloat() / size.y().toFloat()

    actual var resizeable: Boolean = resizeable
        set(value) {
            glfwSetWindowAttrib(id, GLFW_RESIZABLE, if (value) GLFW_TRUE else GLFW_FALSE)
            field = value
        }

    actual var mode: WindowMode = mode
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

    actual var visible: Boolean = true
        set(value) {
            if (value) glfwShowWindow(id)
            else glfwHideWindow(id)
            field = value
        }

    actual var isClosing: Boolean
        get() = glfwWindowShouldClose(id)
        set(value) = glfwSetWindowShouldClose(id, value)

    actual fun close() {
        isClosing = true
    }

    actual var refreshRate: UInt by object : ReadWriteProperty<Window, UInt> {
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

    actual var title: String = title
        set(value) {
            glfwSetWindowTitle(id, value)
            field = value
        }

    actual var cursor: Cursor = cursor
        set(value) {
            field.window = null
            value.window = this
            field = value
        }

    private val xPosBuffer = IntArray(1)
    private val yPosBuffer = IntArray(1)
    private val posBuffer = Vector2i()

    actual var position: Vector2ic
        get() {
            glfwGetWindowPos(id, xPosBuffer, yPosBuffer)
            return posBuffer.apply { x = xPosBuffer[0]; y = yPosBuffer[0] }
        }
        //on Windows, will only succeed if entire window is still within screen space after translation
        set(value) {
            glfwSetWindowSize(id, value.x(), value.y())
            checkError("If you are not on Wayland, be concerned")
        }

    @Deprecated("Only for Java interoperability", level = DeprecationLevel.ERROR)
    constructor(
        size: Vector2ic = DEFAULT, mode: WindowMode = WINDOWED,
        title: String = "Injection Window", refreshRate: Int? = null, position: Vector2ic? = null,
        cursor: Cursor = Cursor(), resizeable: Boolean = false, vSync: Boolean = false, samples: Int = 4,
        createHidden: Boolean = false, transparency: Boolean = false, dummy: Boolean = false
    ) : this(
        size, mode, title, refreshRate?.toUInt(), position, cursor,
        resizeable, vSync, samples.toUInt(), createHidden, transparency
    )

    init {
        check(glfwInit()) { "Unable to initialize glfw library" }

        refreshRate?.let { check(it > 0u) { "Refresh rate must be greater than zero" } }
    }

    actual val keyInputs: MutableList<KeyInputHandler> = mutableListOf()
    actual val cursorInputs: MutableList<CursorInputHandler> = mutableListOf()

    private lateinit var internalScreenBuffer: ScreenBuffer
    actual val screenBuffer: FrameBuffer get() = internalScreenBuffer

    actual var uiScope: CoroutineScope by notNull() //*extremely loud alarm sound*

    actual var scene: Scene? = null

    internal var id: GLFWId by notNull()

    //TODO provide methods to change primary monitor
    internal var monitor: GLFWId by notNull()

    actual var graphicsContext: GraphicsContext by notNull()

    companion object {
        const val MIN_GL_CONTEXT_MAJOR_VERSION = 3
        const val MIN_GL_CONTEXT_MINOR_VERSION = 3

        var USE_RAW_MOUSE_MOTION_IF_AVAILABLE = true
    }

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

//        glfwSetErrorCallback(null);
    }

    private fun initGl() {
        glfwMakeContextCurrent(id)

        val caps = GL.createCapabilities()

        graphicsContext = GraphicsContext(
            glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS),
            glGetInteger(GL_MAX_VERTEX_ATTRIBS)
        )

        graphicsContext.thread = Thread.currentThread()

        GraphicsContext.CONTEXT.set(graphicsContext)

        val glExtensions = glGetString(GL_EXTENSIONS)
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?: listOf()
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

        internalScreenBuffer = ScreenBuffer(Vector2i(this.size))
        graphicsContext.screenBuffer = ScreenBufferInstance(internalScreenBuffer, graphicsContext)
        graphicsContext.activeFramebuffer = graphicsContext.screenBuffer
    }

    private fun configInput() {
        glfwSetFramebufferSizeCallback(id) { _, width, height ->
            (size as Vector2i).set(width, height)

            internalScreenBuffer.size = size
            scene?.let { it.size = size }
        }
//        glfwSetWindowRefreshCallback() //TODO can be used to update during moving, resizing etc.

        if (USE_RAW_MOUSE_MOTION_IF_AVAILABLE && glfwRawMouseMotionSupported()) {
            glfwSetInputMode(id, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }

        glfwSetKeyCallback(id) { windowId, keyCode, scanCode, actionCode, modsCode ->
            if (windowId != id) return@glfwSetKeyCallback

            val key = parseGlfwKey(keyCode)
            if (key == null) {
                logger.warn { "Unknown GLFW key code: $key" }
                return@glfwSetKeyCallback
            }

            val event = KeyEvent(key, parseGlfwAction(actionCode)!!, parseGlfwModifierKeys(modsCode))
            keyInputs.forEach { it.invoke(event) }
            scene?.invoke(event)
        }
        glfwSetCharCallback(id) { windowId, codepoint ->
            if (windowId != id) return@glfwSetCharCallback

            keyInputs.forEach { it.invokeCharacter(codepoint.toChar()) }
            scene?.invokeCharacter(codepoint.toChar())
        }
        val posX = DoubleArray(1)
        val posY = DoubleArray(1)
        glfwSetMouseButtonCallback(id) { window, button, action, glfwMods ->
            if (window != id) return@glfwSetMouseButtonCallback

            val key = parseGlfwKey(button)
            if (key == null) {
                logger.warn { "Unknown GLFW mouse button code: $key" }
                return@glfwSetMouseButtonCallback
            }

            val event = KeyEvent(key, parseGlfwAction(action)!!, parseGlfwModifierKeys(glfwMods))
            keyInputs.forEach { it.invoke(event) }

            glfwGetCursorPos(id, posX, posY)

            cursorInputs.forEach { it.invokeClick(event, posX[0], posY[0]) }
            scene?.invokeClick(event, posX[0], posY[0])
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

    actual fun update(delta: Double) {
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
        check(graphicsContext.thread == null) {
            "Current graphics context must first be detached using detachContext on the old thread"
        }

        glfwMakeContextCurrent(id)
        GL.createCapabilities() //FIXME could these be reused?
        GraphicsContext.CONTEXT.set(graphicsContext)
    }

    @Deprecated(message = "Only call if you know what you are doing.")
    fun detachContext() {
        check(Thread.currentThread() == graphicsContext.thread) {
            "Current graphics context can only be detached from the thread it is attached to"
        }

        glfwMakeContextCurrent(0L)
        GL.setCapabilities(null)
        graphicsContext.thread = null
        GraphicsContext.CONTEXT.set(null)
    }

    override fun dispose() {
        glfwDestroyWindow(id)
        cursor.dispose()
    }

    @Override
    override fun toString(): String =
        "Window{mode=$mode, size=$size, targetFrameRate=$refreshRate, " +
                "vSyncEnabled=$vSync, samples=$samples, title='$title', scene=$scene}"

}

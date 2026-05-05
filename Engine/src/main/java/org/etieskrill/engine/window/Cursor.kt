package org.etieskrill.engine.window

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.window.Cursor.CursorMode.entries
import org.etieskrill.engine.window.Cursor.CursorShape.ARROW
import org.joml.Vector2d
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.memASCII

private val logger = KotlinLogging.logger {}

/**
 * Loads the system's cursors, and activates [shape].
 *
 * If a [CursorShape] could not be loaded for any reason, it will fall back to [CursorShape.ARROW].
 * If [CursorShape.ARROW] is not available, something's fucked.
 *
 * @param shape the activated cursor
 * @return the default cursor object
 * @throws IllegalStateException if no default cursor could be loaded
 */
//TODO constructor which loads some cursor files, may require list of images for cursor modes
//TODO cursor loader
//TODO update disposable once custom cursors are implemented
class Cursor(shape: CursorShape = ARROW) : Disposable {

    init {
        check(glfwInit()) { "Unable to initialize glfw library" }
    }

    internal val id = glfwCreateStandardCursor(shape.glfwShape)

    internal var window: Window? = null
        set(value) {
            value?.let { glfwSetCursor(it.id, id) }
            field = value
        }

    var mode: CursorMode
        get() {
            check(window != null) { "Cursor is not assigned to window" }
            return CursorMode.fromGLFW(glfwGetInputMode(window!!.id, GLFW_CURSOR))!!
        }
        set(value) {
            check(window != null) { "Cursor is not assigned to window" }
            glfwSetInputMode(window!!.id, GLFW_CURSOR, value.glfwMode)
        }

    var shape: CursorShape = shape
        set(value) {
            check(window != null) { "Cursor is not assigned to window" }
            glfwSetCursor(window!!.id, defaultCursors[value] ?: defaultCursors[ARROW]!!)
            field = value
        }

    private val posx = DoubleArray(1)
    private val posy = DoubleArray(1)

    var position: Vector2d
        get() {
            check(window != null) { "Cursor is not assigned to window" }
            glfwGetCursorPos(window!!.id, posx, posy)
            return Vector2d(posx[0], posy[0])
        }
        set(value) {
            check(window != null) { "Cursor is not assigned to window" }
            glfwSetCursorPos(window!!.id, value.x, value.y)
        }

    companion object {
        val defaultCursors: Map<CursorShape, Long> by lazy { loadDefaultCursors() }

        private fun loadDefaultCursors(): Map<CursorShape, Long> {
            var pointer = BufferUtils.createPointerBuffer(1)

            val defaultCursors = mutableMapOf<CursorShape, Long>()

            var arrowCursor = glfwCreateStandardCursor(ARROW.glfwShape)
            var error = Window.GLFWError.fromGLFW(glfwGetError(pointer))
            if (error != Window.GLFWError.NO_ERROR) {
                var message = memASCII(pointer.get())
                throw IllegalStateException("Default cursor shape ARROW could not be created: [GLFW] $message")
            }

            defaultCursors[ARROW] = arrowCursor

            for (cursorShape in CursorShape.entries.filterNot { it == ARROW }) {
                var cursor = glfwCreateStandardCursor(cursorShape.glfwShape)

                error = Window.GLFWError.fromGLFW(glfwGetError(pointer))
                if (error != Window.GLFWError.NO_ERROR) {
                    val message = memASCII(pointer.get())
                    logger.warn { "Failed to create cursor shape $cursorShape, using fallback of ARROW instead [$error]: $message" }
                    pointer.rewind()

                    cursor = Companion.defaultCursors[ARROW]!!
                }

                defaultCursors[cursorShape] = cursor
            }

            return defaultCursors
        }
    }

    enum class CursorShape(val glfwShape: Int) {
        ARROW(GLFW_ARROW_CURSOR),
        IBEAM(GLFW_IBEAM_CURSOR),
        CROSSHAIR(GLFW_CROSSHAIR_CURSOR),
        POINTING_HAND(GLFW_POINTING_HAND_CURSOR),
        RESIZE_EW(GLFW_RESIZE_EW_CURSOR),
        RESIZE_NS(GLFW_RESIZE_NS_CURSOR),
        RESIZE_NWSE(GLFW_RESIZE_NWSE_CURSOR),
        RESIZE_NESW(GLFW_RESIZE_NESW_CURSOR),
        RESIZE_ALL(GLFW_RESIZE_ALL_CURSOR),
        NOT_ALLOWED(GLFW_NOT_ALLOWED_CURSOR);
    }

    enum class CursorMode(val glfwMode: Int) {
        /**
         * Movement is not restricted and normal cursor is shown.
         */
        NORMAL(GLFW_CURSOR_NORMAL),

        /**
         * Movement is not restricted, but the cursor is not visible while hovering over the window.
         */
        HIDDEN(GLFW_CURSOR_HIDDEN),

        /**
         * Cursor movement is locked to the window, and movement is fed into a virtual unlimited cursor space. Use
         * for mouse motion based camera control and the likes.
         */
        DISABLED(GLFW_CURSOR_DISABLED),

        /**
         * Restricts cursor movement to the window, but otherwise behaves normally.
         */
        CAPTURED(GLFW_CURSOR_CAPTURED);

        companion object {
            fun fromGLFW(glfwCursorMode: Int): CursorMode? = entries.find { it.glfwMode == glfwCursorMode }
        }
    }

    fun enable() {
        mode = CursorMode.NORMAL
    }

    fun hide() {
        mode = CursorMode.HIDDEN
    }

    fun disable() {
        mode = CursorMode.DISABLED
    }

    fun capture() {
        mode = CursorMode.CAPTURED
    }

    override fun dispose() {
        //TODO cursor loader
        defaultCursors.values.forEach { glfwDestroyCursor(it) }
    }

}

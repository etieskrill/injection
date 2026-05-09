package org.etieskrill.engine.graphics.gl.exception

import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C.GL_INVALID_FRAMEBUFFER_OPERATION
import org.lwjgl.opengl.GL45C.GL_CONTEXT_LOST

open class GLException(
    message: String? = null,
    cause: Throwable? = null,
    protected open val infoLog: String? = null,
    protected open val errorCode: GLError? = null
) : RuntimeException(message, cause) {
    @OptIn(ExperimentalStdlibApi::class)
    override val message: String?
        get() = buildString {
            append(super.message)
            errorCode?.let { append(": 0x${it.glErrorCode.toHexString().uppercase()} ${it.message}") }
            infoLog?.let { append("\n$it") }
        }
}

enum class GLError(val glErrorCode: Int, val message: String) {
    NO_ERROR(GL_NO_ERROR, ""),
    INVALID_ENUM(GL_INVALID_ENUM, "Used an invalid enum"),
    INVALID_VALUE(GL_INVALID_VALUE, "Set an invalid value"),
    INVALID_OPERATION(GL_INVALID_OPERATION, "Applied an invalid operation"),
    STACK_OVERFLOW(GL_STACK_OVERFLOW, "Stack overflow"),
    STACK_UNDERFLOW(GL_STACK_UNDERFLOW, "Stack underflow"),
    OUT_OF_MEMORY(GL_OUT_OF_MEMORY, "Ran out of memory"),
    INVALID_FRAMEBUFFER_OPERATION(GL_INVALID_FRAMEBUFFER_OPERATION, "Invalid operation was called on a framebuffer"),
    CONTEXT_LOST(GL_CONTEXT_LOST, "FATAL: The OpenGL context was lost");

    companion object {
        fun fromGL(glErrorCode: Int) = entries.find { it.glErrorCode == glErrorCode }
    }
}

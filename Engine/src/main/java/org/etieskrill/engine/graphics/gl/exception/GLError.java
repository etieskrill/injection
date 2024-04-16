package org.etieskrill.engine.graphics.gl.exception;

import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.GL_INVALID_FRAMEBUFFER_OPERATION;
import static org.lwjgl.opengl.GL45C.GL_CONTEXT_LOST;

public enum GLError {
    NO_ERROR(GL_NO_ERROR, ""),
    INVALID_ENUM(GL_INVALID_ENUM, "Used an invalid enum"),
    INVALID_VALUE(GL_INVALID_VALUE, "Set an invalid value"),
    INVALID_OPERATION(GL_INVALID_OPERATION, "Applied an invalid operation"),
    STACK_OVERFLOW(GL_STACK_OVERFLOW, "Stack overflow"),
    STACK_UNDERFLOW(GL_STACK_UNDERFLOW, "Stack underflow"),
    OUT_OF_MEMORY(GL_OUT_OF_MEMORY, "Ran out of memory"),
    INVALID_FRAMEBUFFER_OPERATION(GL_INVALID_FRAMEBUFFER_OPERATION, "Invalid operation was called on a framebuffer"),
    CONTEXT_LOST(GL_CONTEXT_LOST, "FATAL: The OpenGL context was lost");

    private final int glErrorCode;
    private final String message;

    GLError(int glErrorCode, String message) {
        this.glErrorCode = glErrorCode;
        this.message = message;
    }

    public static @Nullable GLError toError(int glErrorCode) {
        if (glErrorCode == GL_NO_ERROR)
            return NO_ERROR;

        for (GLError value : values()) {
            if (value.gl() == glErrorCode)
                return value;
        }
        return null;
    }

    public int gl() {
        return getGlErrorCode();
    }

    public int getGlErrorCode() {
        return glErrorCode;
    }

    public String getMessage() {
        return message;
    }
}
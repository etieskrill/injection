package org.etieskrill.engine.graphics.gl;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glGetError;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GL45C.GL_CONTEXT_LOST;

public class GLUtils {

    private static final Logger logger = LoggerFactory.getLogger(GLUtils.class);

    public static boolean checkError() {
        return checkError("OpenGL Error");
    }

    /**
     * Checks if an error has occurred since the last time of calling and logs a generic description prepended by a
     * message specified by the {@code header} parameter.
     * <p>
     * If the debugger was attached to a log stream with {@link GLUtils#addDebugLogging()}, this method will never catch
     * an error.
     *
     * @param header message prefix in case of error
     * @return {@code true} if there is no error, {@code false} otherwise
     */
    public static boolean checkError(String header) {
        GLCapabilities.initialize();

        int error = getError();
        if (error == ErrorCode.NO_ERROR.gl()) return true;

        ErrorCode glError = ErrorCode.fromGLErrorCode(error);
        logger.warn("{}: {}", header, requireNonNull(glError).getMessage());
        return false;
    }

    public static boolean checkErrorThrowing() {
        return checkErrorThrowing("OpenGL Error");
    }

    public static boolean checkErrorThrowing(String header) {
        return checkErrorThrowing(header, IllegalStateException::new);
    }

    /**
     * Checks if an error has occurred since the last time of calling and throws an {@code Exception} of the specified
     * type containing a generic description prepended by a message specified by the {@code header} parameter.
     * <p>
     * If the debugger was attached to a log stream with {@link GLUtils#addDebugLogging()}, this method will never catch
     * an error.
     *
     * @param header    message prefix in case of error
     * @param exception a constructor for an exception with a single message argument
     * @param <E>       the type of exception to throw
     * @return {@code true} if there is no error, {@code false} otherwise
     */
    public static <E extends Exception> boolean checkErrorThrowing(
            String header,
            Function<String, E> exception
    ) throws E {
        GLCapabilities.initialize();

        int error = getError();
        if (error == ErrorCode.NO_ERROR.gl()) return true;

        ErrorCode glError = ErrorCode.fromGLErrorCode(error);
        throw exception.apply(header + ": " + requireNonNull(glError).getMessage());
    }

    public static void clearError() {
        getError();
    }

    /**
     * Attach a callback to the current OpenGL context, which retrieves the OpenGL log stream, formats the statements,
     * and adds them to the logger in this class at the relevant severity.
     *
     * @see GLUtils#removeDebugLogging()
     */
    public static void addDebugLogging() {
        GLCapabilities.initialize();
        clearError();

        glEnable(GL_DEBUG_OUTPUT);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            switch (DebugSeverity.fromGLDebugSeverity(severity)) {
                case MEDIUM, LOW, NOTIFICATION -> logger.info("{} ({}): {}",
                        DebugSource.fromGLDebugSource(source),
                        DebugType.fromGLDebugType(type),
                        MemoryUtil.memUTF8(message, length));
                case HIGH -> logger.warn("{} ({}): {}",
                        DebugSource.fromGLDebugSource(source),
                        DebugType.fromGLDebugType(type),
                        MemoryUtil.memUTF8(message, length));
                case null -> throw new IllegalStateException("Unexpected severity: " + toHex(severity));
            }
        }, 0L);

        checkError("Failed to attach OpenGL log stream to this logger");
    }

    /**
     * Removes the callback added by {@link GLUtils#addDebugLogging()}.
     *
     * @see GLUtils#addDebugLogging()
     */
    public static void removeDebugLogging() {
        GLCapabilities.initialize();
        clearError();

        glDisable(GL_DEBUG_OUTPUT);
        glDebugMessageCallback(null, 0L);

        checkError("Failed to detach OpenGL log stream from logger");
    }

    private static int getError() {
        GLCapabilities.initialize();
        return glGetError();
    }

    private static String toHex(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    public enum ErrorCode {
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

        ErrorCode(int glErrorCode, String message) {
            this.glErrorCode = glErrorCode;
            this.message = message;
        }

        public static @Nullable ErrorCode fromGLErrorCode(int glErrorCode) {
            if (glErrorCode == GL_NO_ERROR)
                return NO_ERROR;

            for (ErrorCode value : values()) {
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

    public enum DebugSource {
        API(GL_DEBUG_SOURCE_API, "API"),
        WINDOW_SYSTEM(GL_DEBUG_SOURCE_WINDOW_SYSTEM, "Window System"),
        SHADER_COMPILER(GL_DEBUG_SOURCE_SHADER_COMPILER, "Shader Compiler"),
        THIRD_PARTY(GL_DEBUG_SOURCE_THIRD_PARTY, "Third Party"),
        APPLICATION(GL_DEBUG_SOURCE_APPLICATION, "Application"),
        OTHER(GL_DEBUG_SOURCE_OTHER, "Other");

        private final int glSource;
        private final String name;

        DebugSource(int glSource, String name) {
            this.glSource = glSource;
            this.name = name;
        }

        public static @Nullable DebugSource fromGLDebugSource(int glDebugSource) {
            for (DebugSource value : values()) {
                if (value.gl() == glDebugSource)
                    return value;
            }
            return null;
        }

        public int gl() {
            return getGlSource();
        }

        public int getGlSource() {
            return glSource;
        }

        public String getName() {
            return name;
        }
    }

    public enum DebugType {
        ERROR(GL_DEBUG_TYPE_ERROR, "Error"),
        DEPRECATED_BEHAVIOR(GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR, "Deprecated Behaviour"),
        UNDEFINED_BEHAVIOR(GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR, "Undefined Behaviour"),
        PORTABILITY(GL_DEBUG_TYPE_PORTABILITY, "Portability"),
        PERFORMANCE(GL_DEBUG_TYPE_PERFORMANCE, "Performance"),
        OTHER(GL_DEBUG_TYPE_OTHER, "Other"),
        MARKER(GL_DEBUG_TYPE_MARKER, "Marker");

        private final int glType;
        private final String name;

        DebugType(int glType, String name) {
            this.glType = glType;
            this.name = name;
        }

        public static @Nullable DebugType fromGLDebugType(int glDebugType) {
            for (DebugType value : values()) {
                if (value.gl() == glDebugType)
                    return value;
            }
            return null;
        }

        public int gl() {
            return getGlType();
        }

        public int getGlType() {
            return glType;
        }

        public String getName() {
            return name;
        }
    }

    public enum DebugSeverity {
        HIGH(GL_DEBUG_SEVERITY_HIGH, "High"),
        MEDIUM(GL_DEBUG_SEVERITY_MEDIUM, "Medium"),
        LOW(GL_DEBUG_SEVERITY_LOW, "Low"),
        NOTIFICATION(GL_DEBUG_SEVERITY_NOTIFICATION, "Notification");

        private final int glSeverity;
        private final String name;

        DebugSeverity(int glSeverity, String name) {
            this.glSeverity = glSeverity;
            this.name = name;
        }

        public static @Nullable DebugSeverity fromGLDebugSeverity(int glDebugSeverity) {
            for (DebugSeverity value : values()) {
                if (value.gl() == glDebugSeverity)
                    return value;
            }
            return null;
        }

        public int gl() {
            return getGlSeverity();
        }

        public int getGlSeverity() {
            return glSeverity;
        }

        public String getName() {
            return name;
        }
    }

}

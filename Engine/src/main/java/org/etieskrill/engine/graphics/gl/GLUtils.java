package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.gl.exception.GLError;
import org.etieskrill.engine.graphics.gl.exception.GLException;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.graphics.gl.GLUtils.DebugSeverity.toDebugSeverity;
import static org.etieskrill.engine.graphics.gl.GLUtils.DebugSource.toDebugSource;
import static org.etieskrill.engine.graphics.gl.GLUtils.DebugType.toDebugType;
import static org.etieskrill.engine.graphics.gl.exception.GLError.toError;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glGetError;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public final class GLUtils {

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
        if (error == GLError.NO_ERROR.gl()) return true;

        GLError glError = toError(error);
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
        if (error == GLError.NO_ERROR.gl()) return true;

        GLError glError = toError(error);
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
            switch (toDebugSeverity(severity)) {
                case MEDIUM, LOW, NOTIFICATION -> logger.info("{} ({}): {}",
                        toDebugSource(source),
                        toDebugType(type),
                        memUTF8(message, length));
                case HIGH -> logger.warn("{} ({}): {}",
                        toDebugSource(source),
                        toDebugType(type),
                        memUTF8(message, length));
                case null -> throw new GLException("Unexpected severity", severity);
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

        public static @Nullable DebugSource toDebugSource(int glDebugSource) {
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

        public static @Nullable DebugType toDebugType(int glDebugType) {
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

        public static @Nullable DebugSeverity toDebugSeverity(int glDebugSeverity) {
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

    private GLUtils() {
        //Not intended for instantiation
    }

}

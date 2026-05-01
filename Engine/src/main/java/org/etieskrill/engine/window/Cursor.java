package org.etieskrill.engine.window;

import org.etieskrill.engine.common.Disposable;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.memASCII;

//TODO update disposable once custom cursors are implemented
public class Cursor implements Disposable {

    protected CursorShape shape;

    protected Window window;

    private boolean windowSet = false;

    private static Map<CursorShape, Long> defaultCursors;

    private static final Logger logger = LoggerFactory.getLogger(Cursor.class);

    public static Cursor getDefault() {
        return getDefault(CursorShape.ARROW);
    }

    /**
     * Loads the system's cursors, and activates {@code shape}.
     * <p>
     * If any {@link CursorShape} could not be loaded, it will fall back to {@link CursorShape#ARROW}.
     * If {@link CursorShape#ARROW} must always be available, otherwise
     *
     * @param shape the activated cursor
     * @return the default cursor object
     * @throws IllegalStateException if no default cursor could be loaded
     */
    public static Cursor getDefault(CursorShape shape) {
        //TODO perhaps make loaded libraries more safe, for instance via a static init for window/cursor class
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize glfw library");
        return new Cursor(shape);
    }

    //TODO constructor which loads some cursor files, may require list of images for cursor modes
    //TODO cursor loader
    protected Cursor(CursorShape shape) {
        if (defaultCursors == null) {
            var pointer = BufferUtils.createPointerBuffer(1);

            defaultCursors = new HashMap<>();

            var arrowCursor = glfwCreateStandardCursor(CursorShape.ARROW.glfw());
            var error = Window.GLFWError.fromGLFW(glfwGetError(pointer));
            if (error != Window.GLFWError.NO_ERROR) {
                var message = memASCII(pointer.get());
                throw new IllegalStateException("Default cursor shape ARROW could not be created: [" + message + "] " + message);
            }

            defaultCursors.put(CursorShape.ARROW, arrowCursor);

            for (CursorShape cursorShape : Arrays.stream(CursorShape.values()).filter(s -> s != CursorShape.ARROW).toList()) {
                var cursor = glfwCreateStandardCursor(cursorShape.glfw());

                error = Window.GLFWError.fromGLFW(glfwGetError(pointer));
                if (error != Window.GLFWError.NO_ERROR) {
                    var message = memASCII(pointer.get());
                    logger.warn("Failed to create cursor shape {}, using fallback of ARROW instead: [{}] {}", cursorShape, error, message);
                    pointer.rewind();

                    cursor = defaultCursors.get(CursorShape.ARROW);
                }

                defaultCursors.put(cursorShape, cursor);
            }
        }

        this.shape = shape;
    }

    public enum CursorMode {
        /**
         * Movement is not restricted and normal cursor is shown.
         */
        NORMAL,
        /**
         * Movement is not restricted, but the cursor is not visible while hovering over the window.
         */
        HIDDEN,
        /**
         * Cursor movement is locked to the window, and movement is fed into a virtual unlimited cursor space. Use
         * for mouse motion based camera control and the likes.
         */
        DISABLED,
        /**
         * Restricts cursor movement to the window, but otherwise behaves normally.
         */
        CAPTURED;

        public int glfw() {
            return switch (this) {
                case NORMAL -> GLFW_CURSOR_NORMAL;
                case HIDDEN -> GLFW_CURSOR_HIDDEN;
                case DISABLED -> GLFW_CURSOR_DISABLED;
                case CAPTURED -> GLFW_CURSOR_CAPTURED;
            };
        }

        public static @Nullable CursorMode fromGLFW(int glfwCursorMode) {
            for (CursorMode mode : CursorMode.values()) {
                if (mode.glfw() == glfwCursorMode) return mode;
            }
            return null;
        }
    }

    public CursorMode getMode() {
        checkWindow();
        return CursorMode.fromGLFW(glfwGetInputMode(window.getID(), GLFW_CURSOR));
    }

    public void setMode(CursorMode mode) {
        checkWindow();
        glfwSetInputMode(window.getID(), GLFW_CURSOR, mode.glfw());
    }

    public void enable() {
        setMode(CursorMode.NORMAL);
    }

    public void hide() {
        setMode(CursorMode.HIDDEN);
    }

    public void disable() {
        setMode(CursorMode.DISABLED);
    }

    public void capture() {
        setMode(CursorMode.CAPTURED);
    }

    public enum CursorShape {
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

        private final int glfw;

        CursorShape(int glfw) {
            this.glfw = glfw;
        }

        public int glfw() {
            return glfw;
        }
    }

    public void setShape(CursorShape shape) {
        checkWindow();
        this.shape = shape;
        glfwSetCursor(window.getID(), defaultCursors.get(this.shape));
    }

    private final double[] posx = new double[1], posy = new double[1];

    public Vector2d getPosition() {
        checkWindow();
        glfwGetCursorPos(window.getID(), posx, posy);
        return new Vector2d(posx[0], posy[0]);
    }

    public void setPosition(Vector2d position) {
        checkWindow();
        glfwSetCursorPos(window.getID(), position.x(), position.y());
    }

    private void checkWindow() {
        if (!windowSet) throw new IllegalStateException("Cursor is not assigned to window");
    }

    Cursor setWindow(Window window) {
        if (windowSet) throw new UnsupportedOperationException("Cursor is already assigned to window");
        this.window = window;
        windowSet = true;

        setShape(shape);

        return this;
    }

    void unsetWindow() {
        if (!windowSet) throw new UnsupportedOperationException("Cursor is not assigned to window");
        this.window = null;
        windowSet = false;
    }

    @Override
    public void dispose() {
        //TODO cursor loader
        for (long cursorId : new HashSet<>(defaultCursors.values())) {
            glfwDestroyCursor(cursorId);
        }
    }

}

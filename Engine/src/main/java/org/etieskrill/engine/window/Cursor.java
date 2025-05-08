package org.etieskrill.engine.window;

import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

//TODO update disposable once custom cursors are implemented
public class Cursor implements Disposable {

    protected final long cursorId;

    protected Window window;

    private boolean windowSet = false;

    public static Cursor getDefault() {
        return getDefault(CursorShape.ARROW);
    }

    public static Cursor getDefault(CursorShape shape) {
        //TODO perhaps make loaded libraries more safe, for instance via a static init for window/cursor class
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize glfw library");
        return new Cursor(shape);
    }

    //TODO constructor which loads some cursor files, may require list of images for cursor modes
    protected Cursor(CursorShape shape) {
        this(glfwCreateStandardCursor(shape.glfw()));
    }

    protected Cursor(long cursorId) {
        this.cursorId = cursorId;
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

    public void setShape(CursorShape shape) { //TODO create internal list of cursors for this
        throw new UnsupportedOperationException("Not implemented yet");

//        checkWindow();
//        int glfwShape = switch (shape) {
//            case ARROW -> GLFW_ARROW_CURSOR;
//            case IBEAM -> GLFW_IBEAM_CURSOR;
//            case CROSSHAIR -> GLFW_CROSSHAIR_CURSOR;
//            case POINTING_HAND -> GLFW_POINTING_HAND_CURSOR;
//            case RESIZE_EW -> GLFW_RESIZE_EW_CURSOR;
//            case RESIZE_NS -> GLFW_RESIZE_NS_CURSOR;
//            case RESIZE_NWSE -> GLFW_RESIZE_NWSE_CURSOR;
//            case RESIZE_NESW -> GLFW_RESIZE_NESW_CURSOR;
//            case RESIZE_ALL -> GLFW_RESIZE_ALL_CURSOR;
//            case NOT_ALLOWED -> GLFW_NOT_ALLOWED_CURSOR;
//        };
//
//        glfwCreateStandardCursor(glfwShape);
//        glfwSetInputMode(window, );
    }

    private final double[] posx = new double[1], posy = new double[1];

    public Vector2d getPosition() {
        checkWindow();
        glfwGetCursorPos(window.getID(), posx, posy);
        return new Vector2d(posx[0], posy[0]);
    }

    private void checkWindow() {
        if (!windowSet) throw new IllegalStateException("Cursor is not assigned to window");
    }

    long getId() {
        return cursorId;
    }

    Cursor setWindow(Window window) {
        if (windowSet) throw new UnsupportedOperationException("Cursor is already assigned to window");
        this.window = window;
        windowSet = true;
        return this;
    }

    @Override
    public void dispose() {
        if (cursorId != NULL) glfwDestroyCursor(cursorId);
    }

}

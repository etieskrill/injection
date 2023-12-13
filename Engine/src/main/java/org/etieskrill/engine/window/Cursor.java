package org.etieskrill.engine.window;

import glm_.vec2.Vec2;
import org.etieskrill.engine.Disposable;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

//TODO update disposable once custom cursors are implemented
public class Cursor implements Disposable {
    
    protected final long cursor;
    
    protected Window window;
    
    private boolean windowSet = false;
    
    public static Cursor getDefault() {
        //TODO perhaps make loaded libraries more safe, for instance via a static init for window/cursor class
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize glfw library");
        return new Cursor();
    }
    
    //TODO constructor which loads some cursor files, may require list of images for cursor modes
    protected Cursor() {
        this(glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
    }
    
    protected Cursor(long cursor) {
        this.cursor = cursor;
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
        };
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
        ARROW,
        IBEAM,
        CROSSHAIR,
        POINTING_HAND,
        RESIZE_EW,
        RESIZE_NS,
        RESIZE_NWSE,
        RESIZE_NESW,
        RESIZE_ALL,
        NOT_ALLOWED
    }
    
    public void setShape(CursorShape shape) {
        checkWindow();
        int glfwShape = switch (shape) {
            case ARROW -> GLFW_ARROW_CURSOR;
            case IBEAM -> GLFW_IBEAM_CURSOR;
            case CROSSHAIR -> GLFW_CROSSHAIR_CURSOR;
            case POINTING_HAND -> GLFW_POINTING_HAND_CURSOR;
            case RESIZE_EW -> GLFW_RESIZE_EW_CURSOR;
            case RESIZE_NS -> GLFW_RESIZE_NS_CURSOR;
            case RESIZE_NWSE -> GLFW_RESIZE_NWSE_CURSOR;
            case RESIZE_NESW -> GLFW_RESIZE_NESW_CURSOR;
            case RESIZE_ALL -> GLFW_RESIZE_ALL_CURSOR;
            case NOT_ALLOWED -> GLFW_NOT_ALLOWED_CURSOR;
        };
        
        //glfwCreateStandardCursor(glfwShape);
        //glfwSetInputMode(window, );
    }

    public Vec2 getPosition() {
        checkWindow();
        double[] posx = new double[1], posy = new double[1];
        glfwGetCursorPos(window.getID(), posx, posy);
        return new Vec2(posx[0], posy[0]);
    }
    
    private void checkWindow() {
        if (!windowSet) throw new IllegalStateException("Cursor is not assigned to window");
    }
    
    long getId() {
        return cursor;
    }
    
    Cursor setWindow(Window window) {
        if (windowSet) throw new UnsupportedOperationException("Cursor is already assigned to window");
        this.window = window;
        windowSet = true;
        return this;
    }
    
    @Override
    public void dispose() {
        if (cursor != NULL) glfwDestroyCursor(cursor);
    }
    
}

package org.etieskrill.engine.window;

import glm_.vec2.Vec2;
import org.etieskrill.engine.scene._2d.Stage;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.Platform;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    
    public static boolean USE_RAW_MOUSE_MOTION_IF_AVAILABLE = true;
    
    private long window;
    
    private WindowMode mode;
    private long monitor;
    private GLFWVidMode videoMode;
    private WindowSize size;
    private Vec2 position;
    private float targetFrameRate;
    private String title;
    
    private Cursor cursor;
    
    private Stage stage;
    
    private boolean built = false;
    
    public enum WindowMode {
        FULLSCREEN,
        BORDERLESS,
        WINDOWED
    }
    
    public enum WindowSize {
        HD(720, 1280),
        FHD(1080, 1920),
        WUXGA(1200, 1920),
        WQHD(1440, 2560),
        UHD(2160, 3840),
        VGA(480, 640),
        SVGA(600, 800),
        XGA(768, 1024),
        UWHD(1080, 2560),
        UWQHD(1440, 3440),
        UHD_4K(2160, 3840),
        THICC(100, 4000),
        
        LARGEST_FIT(0, 0);
        
        WindowSize(final int height, final int width) {
            if (width < height)
                throw new IllegalArgumentException("Height should not be larger than width");
            
            this.width = width;
            this.height = height;
        }
        
        private final int width;
        private final int height;
    
        /**
         * Maximises width over height.
         *
         * @param width the sought after width
         * @param height the sought after height
         * @return the largest viable format, or null if no format is small enough
         */
        public static WindowSize getLargestFit(int width, int height) {
            WindowSize largestFit = WindowSize.values()[0];
            for (WindowSize windowSize : WindowSize.values()) {
                if (windowSize.getWidth() > width || windowSize.getHeight() > height) continue;
                if (largestFit.getWidth() < windowSize.getWidth() || largestFit.getHeight() < windowSize.getHeight())
                    largestFit = windowSize;
            }
            return largestFit;
        }
        
        public Vec2 toVec() {
            return new Vec2(width, height);
        }
        
        public float getAspectRatio() {
            return (float) width / height;
        }
        
        public int getWidth() {
            return width;
        }
    
        public int getHeight() {
            return height;
        }

        @Override
        public String toString() {
            return String.format("%s(%d x %d)", name(), width, height);
        }
    }
    
    Window(WindowMode mode, WindowSize size, Vec2 position, float targetFrameRate, String title, Cursor cursor) {
        this.mode = mode;
        this.size = size;
        this.position = position;
        this.targetFrameRate = targetFrameRate;
        
        this.title = title;
        
        init();
        
        this.cursor = cursor.setWindow(window);
    }
    
    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize glfw library");
        
        glfwSetErrorCallback((retVal, argv) -> {
            PointerBuffer errorMessage = BufferUtils.createPointerBuffer(1);
            throw new IllegalStateException(String.format("GLFW error occurred: %d\nMessage: %s",
                    glfwGetError(errorMessage), errorMessage.getStringASCII()));
        });
        
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        if (Platform.get() == Platform.MACOSX)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    
        monitor = glfwGetPrimaryMonitor();
        if (monitor == NULL)
            throw new IllegalStateException("Could not find primary monitor");
        
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    
        if (mode == null) throw new IllegalArgumentException("Window mode must not be null");
        videoMode = glfwGetVideoMode(monitor);
        if (videoMode == null) {
            System.err.println("Video mode for monitor could not be retrieved");
            size = size == null ? WindowSize.HD : size;
            targetFrameRate = targetFrameRate < 0 ? 60f : targetFrameRate;
        }

        if (size == null)
            throw new IllegalArgumentException("Window size must not be null");
        else if (size == WindowSize.LARGEST_FIT)
            size = WindowSize.getLargestFit(videoMode.width(), videoMode.height());
        
        if (targetFrameRate < 0) targetFrameRate = videoMode.refreshRate();
    
        setMode(mode);
        setRefreshRate(targetFrameRate);//144);

        this.window = glfwCreateWindow(size.getWidth(), size.getHeight(), title,
                switch (mode) {
                    case FULLSCREEN -> monitor;
                    case WINDOWED, BORDERLESS -> NULL;
                },
                NULL);
        if (this.window == NULL)
            throw new IllegalStateException("Could not create glfw window");
    
        //glfwGetWindowMonitor()
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); //TODO why does it break when 4<?
        
        configInput();
        
        setPos(position);
        this.built = true;
        
        setTitle(title);
    }
    
    private void configInput() {
        if (USE_RAW_MOUSE_MOTION_IF_AVAILABLE && glfwRawMouseMotionSupported())
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
    }
    
    public void show() {
        glfwShowWindow(window);
    }
    
    public void hide() {
        glfwHideWindow(window);
    }
    
    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }
    
    //TODO should probably be named more appropriately
    public void update(double delta) {
        if (stage != null) {
            try {
                stage.update(delta);
                stage.render();
            } catch (UnsupportedOperationException ignored) {}
        }
        
        //glfwMakeContextCurrent(window);
        glfwSwapBuffers(window); //Buffers are usually swapped before polling events
        glfwPollEvents(); //Also proves to system that window has not frozen
    }
    
    @Deprecated
    public long getID() {
        return window;
    }
    
    public WindowMode getMode() {
        return mode;
    }
    
    public void setMode(WindowMode mode) {
        this.mode = mode;
        glfwWindowHint(GLFW_DECORATED, switch (mode) {
            case FULLSCREEN, BORDERLESS -> GLFW_FALSE;
            case WINDOWED -> GLFW_TRUE;
        });
    }
    
    public WindowSize getSize() {
        return size;
    }
    
    public void setSize(WindowSize size) {
        this.size = size;
        glfwSetWindowSize(window, size.getWidth(), size.getHeight());
    }
    
    public float getRefreshRate() {
        return targetFrameRate;
    }
    
    public void setRefreshRate(float refreshRate) {
        if (refreshRate < 0)
            throw new IllegalArgumentException("Refresh rate must not be negative");
        this.targetFrameRate = refreshRate;
        glfwWindowHint(GLFW_REFRESH_RATE, (int) targetFrameRate);
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        if (!built)
            throw new IllegalStateException("Window must be fully initialised before renaming");
        this.title = title;
        glfwSetWindowTitle(window, title);
    }
    
    //on Windows, will only succeed if entire window is still within screen space after translation
    public void setPos(Vec2 pos) {
        glfwSetWindowPos(this.window, (int)(float) pos.getX(), (int)(float) pos.getY());
    }
    
    public Cursor getCursor() {
        return cursor;
    }
    
    public Stage getStage() {
        return stage;
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setSize(this.size.toVec());
    }
    
    public static class Cursor {
        
        private long window = 0L;
        
        private boolean windowSet = false;
        
        public static Cursor getDefault() {
            return new Cursor();
        }
        
        private Cursor() {}
        
        public enum CursorMode {
            /**
             * Movement is not restricted and normal cursor is shown.
             */
            NORMAL,
            /**
             * Movement is not restricted, but the cursor is not visible.
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
            CAPTURED
        }
        
        public void setMode(CursorMode mode) {
            checkWindow();
            int glfwMode = switch (mode) {
                case NORMAL -> GLFW_CURSOR_NORMAL;
                case HIDDEN -> GLFW_CURSOR_HIDDEN;
                case DISABLED -> GLFW_CURSOR_DISABLED;
                case CAPTURED -> GLFW_CURSOR_CAPTURED;
            };
            
            glfwSetInputMode(window, GLFW_CURSOR, glfwMode);
        }
        
        public void normal() {
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
        
        private void checkWindow() {
            if (!windowSet) throw new IllegalStateException("Cursor is not assigned to window");
        }
        
        Cursor setWindow(long window) {
            if (windowSet) throw new UnsupportedOperationException("Cursor is already assigned to window");
            this.window = window;
            windowSet = true;
            return this;
        }
        
    }
    
}

package org.etieskrill.engine.window;

import glm_.vec2.Vec2;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.input.KeyInput;
import org.etieskrill.engine.input.InputManager;
import org.etieskrill.engine.scene._2d.Stage;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window implements Disposable {
    
    public static boolean USE_RAW_MOUSE_MOTION_IF_AVAILABLE = true;
    
    private static final Logger logger = LoggerFactory.getLogger(Window.class);
    
    private long window;
    
    private WindowMode mode;
    private long monitor;
    private GLFWVidMode videoMode;
    private WindowSize size;
    private Vec2 position;
    private float targetFrameRate;
    private String title;
    
    private Cursor cursor;
    
    private InputManager inputs;
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
    
    public static class Builder {
        
        private Window.WindowMode mode;
        private Window.WindowSize size;
        private Vec2 position;
        private float refreshRate;
        private String title;
        private Cursor cursor;
        private InputManager inputs;
        
        public Builder() {}
        
        public Builder setMode(Window.WindowMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder setSize(Window.WindowSize size) {
            this.size = size;
            return this;
        }
        
        public Builder setPosition(Vec2 position) {
            this.position = position;
            return this;
        }
        
        public Builder setRefreshRate(float refreshRate) {
            this.refreshRate = refreshRate;
            return this;
        }
        
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }
        
        public Builder setCursor(Cursor cursor) {
            this.cursor = cursor;
            return this;
        }
    
        public Builder setInputManager(InputManager inputs) {
            this.inputs = inputs;
            return this;
        }
    
        public Window build() {
            return new Window(
                    mode != null ? mode : Window.WindowMode.WINDOWED,
                    size != null ? size : Window.WindowSize.LARGEST_FIT,
                    position != null ? position : new Vec2(),
                    refreshRate >= 0 ? refreshRate : GLFW_DONT_CARE,
                    title != null ? title : "Window",
                    cursor != null ? cursor : Cursor.getDefault(),
                    inputs
            );
        }
        
    }
    
    Window(WindowMode mode, WindowSize size, Vec2 position, float targetFrameRate, String title, Cursor cursor,
           InputManager inputs) {
        this.mode = mode;
        this.size = size;
        this.position = position;
        this.targetFrameRate = targetFrameRate;
        
        this.title = title;
        
        this.inputs = inputs;
        
        init();
        
        this.setCursor(cursor.setWindow(window));
        
        PointerBuffer description = BufferUtils.createPointerBuffer(1);
        int err = glfwGetError(description);
        if (err != GLFW_NO_ERROR) {
            logger.warn("Error during window creation: 0x{} {}",
                    Integer.toHexString(err), MemoryUtil.memASCII(description.get()));
        }
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
    
        //TODO more sophisticated & consumer-controlled monitor choice / general window configuration
        monitor = glfwGetPrimaryMonitor();
        if (monitor == NULL)
            throw new IllegalStateException("Could not find primary monitor");
        
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    
        videoMode = glfwGetVideoMode(monitor);
        if (videoMode == null) {
            System.err.println("Video mode for monitor could not be retrieved");
            size = size == null ? WindowSize.HD : size;
            targetFrameRate = targetFrameRate < 0 ? 60f : targetFrameRate;
        }
        
        if (size == WindowSize.LARGEST_FIT)
            size = WindowSize.getLargestFit(videoMode.width(), videoMode.height());
        
        if (targetFrameRate == GLFW_DONT_CARE) targetFrameRate = videoMode.refreshRate();
    
        setMode(mode);
        setRefreshRate(targetFrameRate);

        this.window = glfwCreateWindow(size.getWidth(), size.getHeight(), title,
                switch (mode) {
                    case FULLSCREEN -> monitor;
                    case WINDOWED, BORDERLESS -> NULL;
                },
                NULL);
        if (this.window == NULL)
            throw new IllegalStateException("Could not create glfw window");
    
        glfwMakeContextCurrent(window);
        initGl();
        
        glfwSwapInterval(1); //TODO why does it break when 4<?
        
        configInput();
        
        setPos(position);
        this.built = true;
        
        setTitle(title);
    }
    
    private void configInput() {
        if (USE_RAW_MOUSE_MOTION_IF_AVAILABLE && glfwRawMouseMotionSupported())
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (inputs != null && window == this.window) inputs.invoke(KeyInput.Type.KEY, key, action, mods);
        });
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (inputs != null && window == this.window) inputs.invoke(KeyInput.Type.MOUSE, button, action, mods);
        });
    }
    
    private void initGl() {
        //TODO 1. figure out what this "context" actually is 2. make window creation disjoint from gl context creation
        GL.createCapabilities();
    
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_BLEND);
    
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }
    
    public void show() {
        glfwShowWindow(window);
    }
    
    public void hide() {
        glfwHideWindow(window);
    }
    
    public void close() {
        glfwSetWindowShouldClose(window, true);
    }
    
    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }
    
    //TODO should probably be named more appropriately
    public void update(double delta) {
        if (stage != null) {
            stage.update(delta);
            stage.render();
        }
        
        //glfwMakeContextCurrent(window);
        glfwSwapBuffers(window); //Buffers are usually swapped before polling events
        if (inputs != null) inputs.update(delta);
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
    
    public void setCursor(Cursor cursor) {
        glfwSetCursor(window, cursor.getId());
        this.cursor = cursor;
    }
    
    public Stage getStage() {
        return stage;
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setSize(this.size.toVec());
    }
    
    @Override
    public void dispose() {
        glfwDestroyWindow(window);
        cursor.dispose();
    }
    
}

package org.etieskrill.engine.window;

import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.scene._2d.Node;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.Platform;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    
    private long window;
    
    private WindowMode mode;
    private long monitor;
    private GLFWVidMode videoMode;
    private WindowSize size;
    private Vec2f position;
    private float targetFrameRate;
    private String title;
    
    private Node root;
    
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
    
    public Window(WindowMode mode, WindowSize size, Vec2f position, float targetFrameRate, String title) {
        this.mode = mode;
        this.size = size;
        this.position = position;
        this.targetFrameRate = targetFrameRate;
        
        this.title = title;
        
        init();
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
        setRefreshRate(targetFrameRate);

        window = glfwCreateWindow(size.getWidth(), size.getHeight(), title,
                switch (mode) {
                    case FULLSCREEN -> monitor;
                    case WINDOWED, BORDERLESS -> NULL;
                },
                NULL);
        if (window == NULL)
            throw new IllegalStateException("Could not create glfw window");
    
        //glfwGetWindowMonitor()
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); //TODO why does it break when 4<?
        
        setPos(position);
        this.built = true;
        
        setTitle(title);
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
    public void update(Renderer renderer, ModelFactory models) {
        //glfwMakeContextCurrent(window);
        glfwSwapBuffers(window); //Buffers are usually swapped before polling events
        glfwPollEvents(); //Also proves to system that window has not frozen
        
        if (root != null) root.draw(renderer, models);
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
    public void setPos(Vec2f pos) {
        glfwSetWindowPos(this.window, (int) pos.getX(), (int) pos.getY());
    }
    
    public Node getRoot() {
        return root;
    }
    
    public void setRoot(Node root) {
        this.root = root;
    }
    
}

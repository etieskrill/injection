package org.etieskrill.engine.window;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.Platform;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    
    private long window;
    
    private WindowMode mode;
    private long monitor;
    private WindowSize size;
    private float targetFrameRate;
    private String title;
    
    public enum WindowMode {
        FULLSCREEN,
        BORDERLESS,
        WINDOWED
    }
    
    public enum WindowSize {
        HD(720, 1280),
        FHD(1080, 1920),
        WQHD(1440, 2560),
        UHD(2160, 3840),
        VGA(480, 640),
        SVGA(600, 800),
        XGA(768, 1024),
        UWHD(1080, 2560),
        UWQHD(1440, 3440),
        UHD_4K(2160, 3840);
        
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
    
    public Window(WindowMode mode, WindowSize size, int targetFrameRate) {
        this.mode = mode;
        this.size = size;
        this.targetFrameRate = targetFrameRate;
        
        this.title = "Window";
        
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
    
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        if (Platform.get() == Platform.MACOSX)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    
        monitor = glfwGetPrimaryMonitor();
        if (monitor == NULL)
            throw new IllegalStateException("Could not find primary monitor");
    
        if (mode == null) mode = WindowMode.WINDOWED;
        GLFWVidMode videoMode = glfwGetVideoMode(monitor);
        if (videoMode == null) {
            System.err.println("Video mode for monitor could not be retrieved");
            size = WindowSize.HD;
            targetFrameRate = 60f;
        }
        if (size == null) size = WindowSize.getLargestFit(videoMode.width(), videoMode.height());
        if (targetFrameRate == 0) targetFrameRate = videoMode.refreshRate();
    
        glfwWindowHint(GLFW_DECORATED, switch (mode) {
            case FULLSCREEN, BORDERLESS -> GLFW_FALSE;
            case WINDOWED -> GLFW_TRUE;
        });
        glfwWindowHint(GLFW_REFRESH_RATE, (int) targetFrameRate);
        
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
    }
    
    public void show() {
        glfwShowWindow(window);
    }
    
    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }
    
    //TODO should probably be named more appropriately
    public void update() {
        glfwSwapBuffers(window); //Buffers are usually swapped before polling events
        glfwPollEvents(); //Also proves to system that window has not frozen
    }
    
    @Deprecated
    public long getID() {
        return window;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(window, title);
    }

    public WindowMode getMode() {
        return mode;
    }

    public WindowSize getSize() {
        return size;
    }

    public float getTargetFrameRate() {
        return targetFrameRate;
    }

}

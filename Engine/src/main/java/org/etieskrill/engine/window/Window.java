package org.etieskrill.engine.window;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.input.*;
import org.etieskrill.engine.scene.Scene;
import org.jetbrains.annotations.Contract;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window implements Disposable {
    
    public static boolean USE_RAW_MOUSE_MOTION_IF_AVAILABLE = true;
    
    private static final Logger logger = LoggerFactory.getLogger(Window.class);
    
    private long window;
    
    private WindowMode mode;
    //TODO provide methods to change primary monitor
    private long monitor;
    private WindowSize size;
    private Vector2f position;
    private float targetFrameRate;
    private boolean vSyncEnabled;
    private int samples;
    private String title;
    
    private Cursor cursor;

    private KeyInputHandler keyInputs;
    private CursorInputHandler cursorInputs;
    private Scene scene;
    
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

        @Contract("-> new")
        public Vector2f toVec() {
            return new Vector2f(width, height);
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
        private Vector2f position;

        private float refreshRate;
        private boolean vSyncEnabled;
        private int samples;

        private String title;
        private Cursor cursor;

        private KeyInputHandler keyInputs;
        private CursorInputHandler cursorInputs;
        
        public Builder() {}
        
        public Builder setMode(Window.WindowMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder setSize(Window.WindowSize size) {
            this.size = size;
            return this;
        }
        
        public Builder setPosition(Vector2f position) {
            this.position = position;
            return this;
        }
        
        public Builder setRefreshRate(float refreshRate) {
            this.refreshRate = refreshRate;
            return this;
        }
        
        public Builder setVSyncEnabled(boolean vSyncEnabled) {
            this.vSyncEnabled = vSyncEnabled;
            return this;
        }

        public Builder setSamples(int samples) {
            this.samples = samples;
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

        public Builder setKeyInputHandler(KeyInputHandler keyInputs) {
            this.keyInputs = keyInputs;
            return this;
        }

        public Builder setCursorInputHandler(CursorInputHandler cursorInputs) {
            this.cursorInputs = cursorInputs;
            return this;
        }
    
        public Window build() {
            return new Window(
                    mode != null ? mode : Window.WindowMode.WINDOWED,
                    size != null ? size : Window.WindowSize.LARGEST_FIT,
                    position != null ? position : new Vector2f(),
                    refreshRate >= 0 ? refreshRate : GLFW_DONT_CARE,
                    vSyncEnabled,
                    Math.max(samples, 0),
                    title != null ? title : "Window",
                    cursor != null ? cursor : Cursor.getDefault(),
                    keyInputs,
                    cursorInputs
            );
        }
        
    }

    private Window(
            WindowMode mode,
            WindowSize size, Vector2f position,
            float targetFrameRate, boolean vSyncEnabled, int samples,
            String title,
            Cursor cursor,
            KeyInputHandler keyInputs,
            CursorInputHandler cursorInputs
    ) {
        this.mode = mode;
        this.size = size;
        this.position = position;
        this.targetFrameRate = targetFrameRate;
        this.vSyncEnabled = vSyncEnabled;
        this.samples = samples;
        
        this.title = title;

        this.keyInputs = keyInputs;
        this.cursorInputs = cursorInputs;
        
        init();
        
        this.setCursor(cursor.setWindow(this));
        
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
        
        //TODO perhaps DON'T throw an exception in a callback
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
    
        GLFWVidMode videoMode = glfwGetVideoMode(monitor);
        if (videoMode == null) {
            logger.warn("Video mode for monitor could not be retrieved");
            size = size == null ? WindowSize.HD : size;
            targetFrameRate = targetFrameRate < 0 ? 60f : targetFrameRate;
        }
        
        if (size == WindowSize.LARGEST_FIT)
            size = WindowSize.getLargestFit(videoMode.width(), videoMode.height());
        
        if (targetFrameRate == GLFW_DONT_CARE) targetFrameRate = videoMode.refreshRate();
    
        setMode(mode);
        setRefreshRate(targetFrameRate);

        glfwWindowHint(GLFW_SAMPLES, samples);

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
        
        glfwSwapInterval(vSyncEnabled ? 1 : 0);
        
        configInput();
        
        setPos(position);
        this.built = true;
        
        setTitle(title);
    }

    private void configInput() {
        if (USE_RAW_MOUSE_MOTION_IF_AVAILABLE && glfwRawMouseMotionSupported())
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (keyInputs != null && window == this.window) keyInputs.invoke(Key.Type.KEYBOARD, key, action, mods);
        });
        final double[] posX = new double[1], posY = new double[1]; //does this cause this method's stack to persist?
        glfwSetMouseButtonCallback(window, (window, button, action, glfwMods) -> {
            if (keyInputs != null && window == this.window) keyInputs.invoke(Key.Type.MOUSE, button, action, glfwMods);
            if (window == this.window && (cursorInputs != null || scene != null)) {
                Keys key = Keys.fromGlfw(button);
                if (key == null) return;
                Key keyWithMods = key.withMods(Keys.Mod.fromGlfw(glfwMods));

                glfwGetCursorPos(window, posX, posY);
                if (cursorInputs != null) cursorInputs.invokeClick(keyWithMods, action, posX[0], posY[0]);
                if (scene != null) scene.invokeClick(keyWithMods, action, posX[0], posY[0]);
            }
        });
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (cursorInputs != null && window == this.window) cursorInputs.invokeMove(xpos, ypos);
            if (scene != null && window == this.window) scene.invokeMove(xpos, ypos);
        });
        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            if (cursorInputs != null && window == this.window) cursorInputs.invokeScroll(xoffset, yoffset);
            if (scene != null && window == this.window) scene.invokeScroll(xoffset, yoffset);
        });
    }
    
    private void initGl() {
        GLCapabilities caps = GL.createCapabilities();
//        System.out.println("woooo " + caps.GL_ARB_shading_language_include);

//        int[] buf = new int[1];
//        glGetIntegerv(GL_MAX_GEOMETRY_OUTPUT_VERTICES, buf);
//        System.out.println("max vertices: " + buf[0]); //TODO this kinda stuff needs to be worked into e.g. the geometry shader, but how best to do that?

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_BLEND);
    
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        if (samples > 0) glEnable(GL_MULTISAMPLE);
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
        if (scene != null) {
            scene.update(delta);
            scene.render();
        }

//        glfwMakeContextCurrent(window);
        glfwSwapBuffers(window); //Buffers are usually swapped before polling events
        glfwPollEvents(); //Also proves to system that window has not frozen
        if (keyInputs != null && keyInputs instanceof KeyInputManager manager) manager.update(delta);
    }
    
    //TODO make package-private
    long getID() {
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
    public void setPos(Vector2f pos) {
        glfwSetWindowPos(this.window, (int) pos.x(), (int) pos.y());
    }
    
    public Cursor getCursor() {
        return cursor;
    }
    
    public void setCursor(Cursor cursor) {
        glfwSetCursor(window, cursor.getId());
        this.cursor = cursor;
    }

    public void setKeyInputs(KeyInputHandler keyInputs) {
        this.keyInputs = keyInputs;
    }

    public void setCursorInputs(CursorInputHandler cursorInputs) {
        this.cursorInputs = cursorInputs;
    }

    public Scene getScene() {
        return scene;
    }
    
    public void setScene(Scene scene) {
        this.scene = scene;
        this.scene.setSize(this.size.toVec());
    }
    
    @Override
    public void dispose() {
        glfwDestroyWindow(window);
        cursor.dispose();
    }
    
}

package org.etieskrill.engine.window;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.config.InjectionConfig;
import org.etieskrill.engine.config.SimpleLoggerConfig;
import org.etieskrill.engine.input.*;
import org.etieskrill.engine.scene.Scene;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNullElse;
import static org.etieskrill.engine.window.Window.GLFWError.NO_ERROR;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public class Window implements Disposable {

    static final int MIN_GL_CONTEXT_MAJOR_VERSION = 3;
    static final int MIN_GL_CONTEXT_MINOR_VERSION = 3;

    static {
        InjectionConfig.init();
    }

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

    private final List<KeyInputHandler> keyInputs;
    private final List<CursorInputHandler> cursorInputs;
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
         * @param width  the sought after width
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
        public Vector2i toVec() {
            return new Vector2i(width, height);
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
            return String.format("%s(%dx%d)", name(), width, height);
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

        private final List<KeyInputHandler> keyInputs = new ArrayList<>();
        private final List<CursorInputHandler> cursorInputs = new ArrayList<>();

        public Builder() {
        }

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

        public Builder setKeyInputHandlers(KeyInputHandler... keyInputs) {
            this.keyInputs.clear();
            this.keyInputs.addAll(asList(keyInputs));
            return this;
        }

        public Builder addKeyInputHandler(KeyInputHandler keyInputs) {
            this.keyInputs.add(keyInputs);
            return this;
        }

        public Builder setCursorInputHandler(CursorInputHandler... cursorInputs) {
            this.keyInputs.clear();
            this.cursorInputs.addAll(asList(cursorInputs));
            return this;
        }

        public Builder addCursorInputHandler(CursorInputHandler cursorInputs) {
            this.cursorInputs.add(cursorInputs);
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
            List<KeyInputHandler> keyInputs,
            List<CursorInputHandler> cursorInputs
    ) {
        this.mode = mode;
        this.size = size;
        this.position = position;
        this.targetFrameRate = targetFrameRate;
        this.vSyncEnabled = vSyncEnabled;
        this.samples = samples;

        this.title = title;

        if (keyInputs.stream().anyMatch(Objects::isNull)
                || cursorInputs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Input handlers must not contain null entries");
        }
        this.keyInputs = keyInputs;
        this.cursorInputs = cursorInputs;

        init();

        this.setCursor(cursor.setWindow(this));

        checkErrorThrowing("Error during window creation");

        logger.info("Created window with settings: {}", this);
    }

    @SuppressWarnings("resource")
    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize glfw library");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, MIN_GL_CONTEXT_MAJOR_VERSION);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, MIN_GL_CONTEXT_MINOR_VERSION);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        }

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
        checkErrorThrowing();
        if (this.window == NULL)
            throw new IllegalStateException("Could not create glfw window");

        glfwMakeContextCurrent(window);
        initGl();

        glfwSwapInterval(vSyncEnabled ? 1 : 0);

        configInput();

        setPos(position);
        this.built = true;

        setTitle(title);

        glfwSetErrorCallback(null);
    }

    @SuppressWarnings("resource")
    private void configInput() {
        if (USE_RAW_MOUSE_MOTION_IF_AVAILABLE && glfwRawMouseMotionSupported())
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (window == this.window) {
                keyInputs.forEach(keyInputHandlers ->
                        keyInputHandlers.invoke(Key.Type.KEYBOARD, key, action, mods));
            }
        });
        final double[] posX = new double[1], posY = new double[1]; //does this cause this method's stack to persist?
        glfwSetMouseButtonCallback(window, (window, button, action, glfwMods) -> {
            if (window == this.window) keyInputs.forEach(keyInputHandler ->
                    keyInputHandler.invoke(Key.Type.MOUSE, button, action, glfwMods));
            if (window == this.window && (cursorInputs != null || scene != null)) {
                Keys key = Keys.fromGlfw(button);
                if (key == null) return;
                Key keyWithMods = key.withMods(Keys.Mod.fromGlfw(glfwMods));

                glfwGetCursorPos(window, posX, posY);
                cursorInputs.forEach(cursorInputHandler ->
                        cursorInputHandler.invokeClick(keyWithMods, action, posX[0], posY[0]));
                if (scene != null) scene.invokeClick(keyWithMods, action, posX[0], posY[0]);
            }
        });
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (window == this.window) cursorInputs.forEach(cursorInputHandler ->
                    cursorInputHandler.invokeMove(xpos, ypos));
            if (scene != null && window == this.window) scene.invokeMove(xpos, ypos);
        });
        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            if (window == this.window) cursorInputs.forEach(cursorInputHandler ->
                    cursorInputHandler.invokeScroll(xoffset, yoffset));
            if (scene != null && window == this.window) scene.invokeScroll(xoffset, yoffset);
        });
    }

    private void initGl() {
        GLCapabilities caps = GL.createCapabilities();
//        System.out.println("woooo " + caps.GL_ARB_shading_language_include);

//        int[] buf = new int[1];
//        glGetIntegerv(GL_MAX_GEOMETRY_OUTPUT_VERTICES, buf);
//        System.out.println("max vertices: " + buf[0]); //TODO this kinda stuff needs to be worked into e.g. the geometry shader, but how best to do that?

        String[] glExtensions = requireNonNullElse(glGetString(GL_EXTENSIONS), "").split(" ");
        logger.debug("""
                        \n\tGL context:
                        \t\tversion: {}
                        \t\tshading language version: {}
                        \t\textensions ({} total): {}
                        \t\trenderer: {}
                        \t\tvendor: {}""",
                glGetString(GL_VERSION), glGetString(GL_SHADING_LANGUAGE_VERSION),
                glExtensions.length == 1 && glExtensions[0].isBlank() ? 0 : glExtensions.length, glExtensions,
                glGetString(GL_RENDERER), glGetString(GL_VENDOR));

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

        glfwSwapBuffers(window); //Buffers are usually swapped before polling events
        glfwPollEvents(); //Also proves to system that window has not frozen
        for (KeyInputHandler keyInputs : keyInputs) {
            if (keyInputs instanceof KeyInputManager manager) manager.update(delta);
        }
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

    public void addKeyInputs(KeyInputHandler keyInputs) {
        this.keyInputs.add(keyInputs);
    }

    public void setKeyInputs(KeyInputHandler... keyInputs) {
        this.keyInputs.clear();
        this.keyInputs.addAll(asList(keyInputs));
    }

    public void clearKeyInputs() {
        keyInputs.clear();
    }

    public void addCursorInputs(CursorInputHandler cursorInputs) {
        this.cursorInputs.add(cursorInputs);
    }

    public void setCursorInputs(CursorInputHandler... cursorInputs) {
        this.keyInputs.clear();
        this.cursorInputs.addAll(asList(cursorInputs));
    }

    public void clearCursorInputs() {
        cursorInputs.clear();
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        this.scene.setSize(this.size.toVec());
    }

    private static void checkError() {
        PointerBuffer description = BufferUtils.createPointerBuffer(1);
        GLFWError error = GLFWError.fromGLFW(glfwGetError(description));
        if (error != NO_ERROR) {
            logger.warn("GLFW error occurred: {} {}", error.toString(), memUTF8(description.get()));
        }
    }

    private static void checkErrorThrowing() {
        checkErrorThrowing(null);
    }

    private static void checkErrorThrowing(@Nullable String header) {
        PointerBuffer description = BufferUtils.createPointerBuffer(1);
        GLFWError error = GLFWError.fromGLFW(glfwGetError(description));
        if (error != NO_ERROR) {
            throw new IllegalStateException(
                    header != null ? header + ":\n" : "GLFW error occurred:\n"
                            + error.toString() + " " + memUTF8(description.get())
            );
        }
    }

    enum GLFWError {
        NO_ERROR(GLFW_NO_ERROR),
        NOT_INITIALIZED(GLFW_NOT_INITIALIZED),
        NO_CURRENT_CONTEXT(GLFW_NO_CURRENT_CONTEXT),
        INVALID_ENUM(GLFW_INVALID_ENUM),
        INVALID_VALUE(GLFW_INVALID_VALUE),
        OUT_OF_MEMORY(GLFW_OUT_OF_MEMORY),
        API_UNAVAILABLE(GLFW_API_UNAVAILABLE),
        VERSION_UNAVAILABLE(GLFW_VERSION_UNAVAILABLE),
        PLATFORM_ERROR(GLFW_PLATFORM_ERROR),
        FORMAT_UNAVAILABLE(GLFW_FORMAT_UNAVAILABLE),
        NO_WINDOW_CONTEXT(GLFW_NO_WINDOW_CONTEXT),
        CURSOR_UNAVAILABLE(GLFW_CURSOR_UNAVAILABLE),
        FEATURE_UNAVAILABLE(GLFW_FEATURE_UNAVAILABLE),
        FEATURE_UNIMPLEMENTED(GLFW_FEATURE_UNIMPLEMENTED),
        PLATFORM_UNAVAILABLE(GLFW_PLATFORM_UNAVAILABLE);

        private final int glfwError;

        GLFWError(int glfwError) {
            this.glfwError = glfwError;
        }

        public static GLFWError fromGLFW(int glfwError) {
            if (glfwError == NO_ERROR.glfw())
                return NO_ERROR;
            for (GLFWError value : values()) {
                if (value.glfw() == glfwError)
                    return value;
            }
            throw new IllegalStateException("Unknown GLFW error: 0x" + Integer.toHexString(glfwError));
        }

        public int glfw() {
            return glfwError;
        }
    }

    @Override
    public void dispose() {
        glfwDestroyWindow(window);
        cursor.dispose();
    }

    @Override
    public String toString() {
        return "Window{" +
                "mode=" + mode +
                ", size=" + size +
                ", targetFrameRate=" + targetFrameRate +
                ", vSyncEnabled=" + vSyncEnabled +
                ", samples=" + samples +
                ", title='" + title + '\'' +
                ", scene=" + scene +
                '}';
    }

}

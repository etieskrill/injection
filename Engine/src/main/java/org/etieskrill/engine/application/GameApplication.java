package org.etieskrill.engine.application;

import lombok.Getter;
import org.etieskrill.engine.config.InjectionConfig;
import org.etieskrill.engine.entity.system.EntitySystem;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.FixedArrayDeque;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.opengl.GL;

import java.util.ArrayDeque;

import static lombok.AccessLevel.PROTECTED;
import static org.etieskrill.engine.time.TimeResolutionUtils.resetSystemTimeResolution;
import static org.etieskrill.engine.time.TimeResolutionUtils.setSystemTimeResolution;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

public abstract class GameApplication {

    private static final int SYSTEM_TIME_RESOLUTION_MILLIS = 1;

    protected final Window window;
    protected final LoopPacer pacer;

    protected final GLRenderer renderer;

    protected final @Getter EntitySystem entitySystem;

    private @Getter(PROTECTED) double avgCpuTime;
    private final ArrayDeque<Double> cpuTimes;

    static {
        InjectionConfig.init();
    }

    public GameApplication(Window window) {
        this.window = window;
        this.window.addKeyInputs((type, key, action, modifiers) -> {
            if (type == Key.Type.KEYBOARD
                    && key == Keys.ESC.getInput().getValue()
                    && modifiers == Keys.Mod.SHIFT.getGlfwKey()) {
                window.close();
                return true;
            }
            return false;
        });
        this.pacer = new SystemNanoTimePacer(1d / window.getRefreshRate());
        this.renderer = new GLRenderer();
        this.entitySystem = new EntitySystem();
        this.cpuTimes = new FixedArrayDeque<>((int) window.getRefreshRate());

        try {
            init();
            _loop();
        } finally {
            terminate();
        }
    }

    protected abstract void init();

    protected void _loop() {
        setSystemTimeResolution(SYSTEM_TIME_RESOLUTION_MILLIS);
        pacer.start();
        while (!window.shouldClose()) {
            doLoop();
            pacer.nextFrame();
        }
    }

    @VisibleForTesting
    protected void doLoop() {
        renderer.nextFrame();

        long time = System.nanoTime();

        double delta = pacer.getDeltaTimeSeconds();
        loop(delta);
        entitySystem.update(delta);
        render();
        window.update(delta);

        cpuTimes.push((System.nanoTime() - time) / 1_000_000d);
        avgCpuTime = cpuTimes.stream()
                .mapToDouble(value -> value)
                .average()
                .orElse(0);
    }

    protected abstract void loop(double delta);

    protected void render() {
    }

    protected void terminate() {
        resetSystemTimeResolution(SYSTEM_TIME_RESOLUTION_MILLIS);
        window.close();
        window.dispose();
        Loaders.disposeDefaultLoaders();
        TrueTypeFont.disposeLibrary();
        GL.destroy();
        glfwTerminate();
    }

}

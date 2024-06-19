package org.etieskrill.engine.application;

import org.etieskrill.engine.entity.system.EntitySystem;
import org.etieskrill.engine.graphics.gl.GLRenderer;
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

import static org.lwjgl.glfw.GLFW.glfwTerminate;

public abstract class GameApplication {

    protected final Window window;
    protected final LoopPacer pacer;

    protected final GLRenderer renderer;

    protected final EntitySystem entitySystem;

    private double avgCpuTime;
    private final ArrayDeque<Double> cpuTimes;

    public GameApplication(int frameRate, Window window) {
        this.window = window;
        this.window.setRefreshRate(frameRate);
        this.window.addKeyInputs((type, key, action, modifiers) -> {
            if (type == Key.Type.KEYBOARD
                    && key == Keys.ESC.getInput().getValue()
                    && modifiers == Keys.Mod.SHIFT.getGlfwKey()) {
                window.close();
                return true;
            }
            return false;
        });
        this.pacer = new SystemNanoTimePacer(1d / frameRate);
        this.renderer = new GLRenderer();
        this.entitySystem = new EntitySystem();
        this.cpuTimes = new FixedArrayDeque<>(frameRate);

        try {
            init();
            _loop();
        } finally {
            terminate();
        }
    }

    protected abstract void init();

    protected void _loop() {
        pacer.start();
        while (!window.shouldClose()) {
            doLoop();
            pacer.nextFrame();
        }
        window.dispose();
    }

    @VisibleForTesting
    protected void doLoop() {
        renderer.prepare();

        long time = System.nanoTime();

        double delta = pacer.getDeltaTimeSeconds();
        loop(delta);
        entitySystem.update(delta);
        window.update(delta);

        cpuTimes.push((System.nanoTime() - time) / 1_000_000d);
        avgCpuTime = cpuTimes.stream()
                .mapToDouble(value -> value)
                .average()
                .orElse(0);
    }

    protected abstract void loop(double delta);

    protected void terminate() {
        window.close();
        Loaders.disposeDefaultLoaders();
        TrueTypeFont.disposeLibrary();
        GL.destroy();
        glfwTerminate();
    }

    protected double getAvgCpuTime() {
        return avgCpuTime;
    }

}

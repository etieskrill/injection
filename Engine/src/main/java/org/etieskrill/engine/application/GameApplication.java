package org.etieskrill.engine.application;

import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.FixedArrayDeque;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;

import java.util.ArrayDeque;

public abstract class GameApplication {

    protected final Window window;
    protected final LoopPacer pacer;

    protected final Renderer renderer;

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
        this.cpuTimes = new FixedArrayDeque<>(frameRate);

        init();
        _loop();
        terminate();
    }

    protected abstract void init();

    protected void _loop() {
        pacer.start();
        while (!window.shouldClose()) {
            renderer.prepare();

            long time = System.nanoTime();

            double delta = pacer.getDeltaTimeSeconds();
            loop(delta);
            window.update(delta);

            cpuTimes.push((System.nanoTime() - time) / 1000000d);
            avgCpuTime = cpuTimes.stream()
                    .mapToDouble(value -> value)
                    .average()
                    .orElse(0);

            pacer.nextFrame();
        }
        window.dispose();
    }

    protected abstract void loop(double delta);

    protected void terminate() {
        window.close();
        Loaders.disposeDefaultLoaders();
    }

    protected double getAvgCpuTime() {
        return avgCpuTime;
    }

}
package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.gl.DebuggableRenderer;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.scene.component.Stack;
import org.etieskrill.engine.time.LoopPacer;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class DebugOverlay extends Scene {

    private final DebuggableRenderer renderer;
    private final LoopPacer pacer;
    private final Label renderStatistics;

    private double cpuTime;

    public DebugOverlay(GLRenderer renderer, LoopPacer pacer, Vector2fc windowSize) {
        this.renderer = renderer;
        this.pacer = pacer;

        this.renderStatistics = new Label();

        setBatch(new Batch(renderer, renderer).setShader(Shaders.getTextShader()));
        setRoot(getRootNode());
        setCamera(new OrthographicCamera(windowSize).setPosition(new Vector3f(windowSize, 0).mul(.5f)));
    }

    private Node getRootNode() {
        return new Stack(List.of(
                renderStatistics
                        .setAlignment(org.etieskrill.engine.scene.component.Node.Alignment.TOP_LEFT)
                        .setMargin(new Vector4f(10))
        ));
    }

    public void setCpuTime(double cpuTime) {
        this.cpuTime = cpuTime;
    }

    @Override
    public void update(double delta) {
        renderStatistics.setText(
                "Render calls: " + renderer.getRenderCalls() +
                        "\nTriangles drawn: " + renderer.getTrianglesDrawn() +
                        "\nAverage fps: %3.0f (%4.1fms)".formatted(pacer.getAverageFPS(), 1000d / pacer.getAverageFPS()) +
                        "\nCPU time: %4.1fms (%4.1fms gpu sync time)".formatted(cpuTime, renderer.getGpuDelay() / 1000000d) +
                        "\nGPU time: %4.1fms".formatted(renderer.getAveragedGpuTime() / 1000000d)
        );
        super.update(delta);
    }

}

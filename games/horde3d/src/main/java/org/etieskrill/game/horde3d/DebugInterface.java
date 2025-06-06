package org.etieskrill.game.horde3d;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.text.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.window.Window;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.joml.Vector4f;

public class DebugInterface extends Scene {

    private final GLRenderer renderer;
    private final LoopPacer pacer;

    private final Label fpsLabel;

    public DebugInterface(Window window, GLRenderer renderer) {
        this(window.getSize().getVec(), renderer, null);
    }

    public DebugInterface(Vector2ic windowSize, GLRenderer renderer, @Nullable LoopPacer pacer) {
        setBatch(new Batch(renderer, windowSize));
        setCamera(new OrthographicCamera(windowSize));

        this.renderer = renderer;
        this.pacer = pacer;

        this.fpsLabel = new Label("", Fonts.getDefault(36));
        this.fpsLabel.setAlignment(Node.Alignment.TOP_LEFT)
                .setMargin(new Vector4f(10));
        setRoot(new Container(fpsLabel));
    }

    @Override
    public void update(double delta) {
        fpsLabel.setText("Fps: %s\nRender calls: %d\nTriangles: %d".formatted(
                pacer != null ? String.valueOf(Math.round(pacer.getAverageFPS())) : "n/a",
                renderer.getRenderCalls(),
                renderer.getTrianglesDrawn()
        ));
        super.update(delta);
    }

    public Label getFpsLabel() {
        return fpsLabel;
    }

}

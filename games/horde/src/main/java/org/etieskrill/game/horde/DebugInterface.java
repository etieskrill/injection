package org.etieskrill.game.horde;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.window.Window;
import org.joml.Vector4f;

public class DebugInterface extends Scene {

    private final Label fpsLabel;

    public DebugInterface(Window window, GLRenderer renderer) {
        OrthographicCamera uiCamera = new OrthographicCamera(window.getSize().toVec());
        fpsLabel = new Label("", Fonts.getDefault(36));
        fpsLabel.setAlignment(Node.Alignment.TOP_LEFT)
                .setMargin(new Vector4f(10));
        window.setScene(new Scene(new Batch(renderer), new Container(fpsLabel), uiCamera));
    }

    public Label getFpsLabel() {
        return fpsLabel;
    }

}

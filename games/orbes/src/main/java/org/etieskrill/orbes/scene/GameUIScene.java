package org.etieskrill.orbes.scene;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.scene.component.Stack;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class GameUIScene extends Scene {

    private Label scoreLabel;
    private Label fpsLabel;
    private Label timerLabel;

    public GameUIScene(Batch batch, Camera camera, Vector2f windowSize) {
        super(batch, new Container(), camera);
        init(windowSize);
    }

    private void init(Vector2f windowSize) {
        getCamera().setPosition(new Vector3f(windowSize.mul(.5f), 0));

        scoreLabel = new Label("", Fonts.getDefault(48));
        scoreLabel.setAlignment(Node.Alignment.TOP).setMargin(new Vector4f(20));

        fpsLabel = new Label("", Fonts.getDefault(36));
        fpsLabel.setAlignment(Node.Alignment.TOP_LEFT).setMargin(new Vector4f(10));

        timerLabel = new Label("", Fonts.getDefault(64));
        timerLabel.setAlignment(Node.Alignment.BOTTOM).setMargin(new Vector4f(50));

        Stack stack = new Stack(List.of(scoreLabel, fpsLabel, timerLabel));

        setRoot(stack);
    }

    public Label getScoreLabel() {
        return scoreLabel;
    }

    public Label getFpsLabel() {
        return fpsLabel;
    }

    public Label getTimerLabel() {
        return timerLabel;
    }

}

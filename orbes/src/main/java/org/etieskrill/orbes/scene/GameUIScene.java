package org.etieskrill.orbes.scene;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.scene.component.Stack;

import java.util.List;

public class GameUIScene extends Scene {

    private Label scoreLabel;
    private Label fpsLabel;

    public GameUIScene(Batch batch, Camera camera, Vec2 windowSize) {
        super(batch, new Container(), camera);
        init(windowSize);
    }

    private void init(Vec2 windowSize) {
        getCamera().setPosition(new Vec3(windowSize.times(0.5), 0));

        scoreLabel = new Label("", Fonts.getDefault(48));
        scoreLabel.setAlignment(Node.Alignment.TOP).setMargin(new Vec4(20));

        fpsLabel = new Label("", Fonts.getDefault(36));
        fpsLabel.setAlignment(Node.Alignment.TOP_LEFT).setMargin(new Vec4(10));

        Stack stack = new Stack(List.of(scoreLabel, fpsLabel));

        setRoot(stack);
    }

    public Label getScoreLabel() {
        return scoreLabel;
    }

    public Label getFpsLabel() {
        return fpsLabel;
    }

}

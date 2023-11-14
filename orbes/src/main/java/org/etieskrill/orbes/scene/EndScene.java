package org.etieskrill.orbes.scene;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.*;
import org.etieskrill.orbes.Game;

import static java.util.Objects.requireNonNull;

public class EndScene extends Scene {

    private Label statusLabel;
    private Label scoreLabel;

    public enum Status {
        VICTORY,
        TIMEOUT
    }

    public EndScene(Batch batch, Camera camera, Game game) {
        super(batch, new Container(), camera);
        init(game);
    }

    private void init(Game game) {
        statusLabel = (Label) new Label("", Fonts.getDefault(96))
                .setAlignment(Node.Alignment.CENTER)
                .setMargin(new Vec4(20));

        scoreLabel = (Label) new Label("", Fonts.getDefault(48))
                .setAlignment(Node.Alignment.CENTER)
                .setMargin(new Vec4(70, 20, 20, 20));

        Label mainMenuLabel = (Label) new Label("Main Menu", Fonts.getDefault(48))
                .setAlignment(Node.Alignment.CENTER);
        Button mainMenu = (Button) new Button(mainMenuLabel)
                .setAlignment(Node.Alignment.CENTER)
                .setSize(new Vec2(300, 100))
                .setMargin(new Vec4(0));
        mainMenu.setAction(game::showMainMenu);

        setRoot(new VBox(statusLabel, scoreLabel, mainMenu));
    }

    public EndScene setStatus(Status status) {
        switch (requireNonNull(status)) {
            case VICTORY -> statusLabel.setText("You winnered!");
            case TIMEOUT -> statusLabel.setText("Time ran out!");
        }
        return this;
    }

    public EndScene setScore(int score) {
        scoreLabel.setText("U collectered a t0tal of " + score + " orbes");
        return this;
    }

}

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

    private Label label;

    public enum Status {
        VICTORY,
        TIMEOUT
    }

    public EndScene(Batch batch, Camera camera, Game game) {
        super(batch, new Container(), camera);
        init(game);
    }

    private void init(Game game) {
        label = (Label) new Label("", Fonts.getDefault(96))
                .setAlignment(Node.Alignment.CENTER)
                .setMargin(new Vec4(75));

        Label mainMenuLabel = (Label) new Label("Main Menu", Fonts.getDefault(48))
                .setAlignment(Node.Alignment.CENTER);
        Button mainMenu = (Button) new Button(mainMenuLabel)
                .setAlignment(Node.Alignment.CENTER)
                .setSize(new Vec2(300, 100))
                .setMargin(new Vec4(0));
        mainMenu.setAction(game::showMainMenu);

        setRoot(new VBox(label, mainMenu));
    }

    public EndScene setStatus(Status status) {
        switch (requireNonNull(status)) {
            case VICTORY -> label.setText("You winnered!");
            case TIMEOUT -> label.setText("Time ran out!");
        }
        return this;
    }

}

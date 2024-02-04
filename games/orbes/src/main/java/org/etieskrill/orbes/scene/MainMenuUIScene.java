package org.etieskrill.orbes.scene;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Button;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node.Alignment;
import org.etieskrill.engine.scene.component.VBox;
import org.etieskrill.orbes.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMenuUIScene extends Scene {

    private static final Logger logger = LoggerFactory.getLogger(MainMenuUIScene.class);

    private final Font font = Fonts.getDefault(48);

    public MainMenuUIScene(Batch batch, Camera camera, Game game) {
        super(batch, new Container(), camera);
        init(game);
    }

    private void init(Game game) {
        Button start = (Button) new Button((Label) new Label("Start", font).setAlignment(Alignment.CENTER)).setAlignment(Alignment.CENTER).setSize(new Vector2f(300, 100)).setMargin(new Vector4f(10)),
                options = (Button) new Button((Label) new Label("Options", font).setAlignment(Alignment.CENTER)).setAlignment(Alignment.CENTER).setSize(new Vector2f(300, 100)).setMargin(new Vector4f(10)),
                exit = (Button) new Button((Label) new Label("Exit", font).setAlignment(Alignment.CENTER)).setAlignment(Alignment.CENTER).setSize(new Vector2f(300, 100)).setMargin(new Vector4f(10));

        start.setAction(game::showGame);
        options.setAction(() -> logger.info("//TODO options"));
        exit.setAction(() -> game.getWindow().close());

        VBox menu = new VBox(start, options, exit);

        setRoot(menu);
    }

}

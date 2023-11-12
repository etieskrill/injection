package org.etieskrill.orbes.scene;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Button;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.HBox;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.orbes.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.engine.scene.component.Node.Alignment.CENTER;

public class GameUIPauseScene extends Scene {

    private static final Logger logger = LoggerFactory.getLogger(GameUIScene.class);

    public GameUIPauseScene(Batch batch, Camera camera, Game game) {
        super(batch, new Container(), camera);
        init(game);
    }

    private void init(Game game) {
        Button aContinue = new Button((Label) new Label("Continue", Fonts.getDefault(48)).setAlignment(CENTER));
        aContinue.setSize(new Vec2(300, 100)).setAlignment(CENTER).setMargin(new Vec4(10));
        aContinue.setAction(game::unpause);

        Button mainMenu = new Button((Label) new Label("Main Menu", Fonts.getDefault(48)).setAlignment(CENTER));
        mainMenu.setSize(new Vec2(300, 100)).setAlignment(CENTER).setMargin(new Vec4(10));
        mainMenu.setAction(game::showMainMenu);

        HBox menu = new HBox(aContinue, mainMenu);

        setRoot(menu);
    }

}

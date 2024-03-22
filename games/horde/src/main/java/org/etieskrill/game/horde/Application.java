package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;

public class Application extends GameApplication {

    private static final int FRAME_RATE = 60;

    public Application() {
        super(FRAME_RATE, new Window.Builder()
                .setTitle("Horde")
                .setMode(Window.WindowMode.BORDERLESS)
                .setSamples(4)
                .setVSyncEnabled(true)
                .build()
        );
    }

    @Override
    protected void init() {
        window.setScene(new Scene(
                new Batch((GLRenderer) renderer),
                new Container(
                        new Label("Horde", Fonts.getDefault(128)).setAlignment(Node.Alignment.CENTER)),
                new OrthographicCamera(window.getSize().toVec())
                        .setPosition(new Vector3f(window.getSize().toVec().mul(.5f), 0))
        ));
    }

    @Override
    protected void loop(double delta) {
    }

    public static void main(String[] args) {
        new Application();
    }
}

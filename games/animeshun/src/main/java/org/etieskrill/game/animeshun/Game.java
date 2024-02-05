package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;

public class Game {

    private static final int FRAMERATE = 60;

    private Window window;
    private final Renderer renderer = new Renderer();

    private final KeyInputManager controls = Input.of(
            Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(this::terminate)
    );

    private Camera camera;
    private Model vampy;
    private ShaderProgram vampyShader;

    Game() {
        init();
        loop();
        terminate();
    }

    private void init() {
        this.window = new Window.Builder()
                .setTitle("Animeshun yeeees")
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(FRAMERATE)
                .setInputManager(controls)
                .build();

        this.camera = new PerspectiveCamera(window.getSize().toVec());

        this.vampy = Loaders.ModelLoader.get().load("vampy", () -> Model.ofFile("vampire_hip_hop.fbx"));
        this.vampyShader = Loaders.ShaderLoader.get().load("vampyShader", Shaders::getStandardShader);
    }

    private void loop() {
        LoopPacer pacer = new SystemNanoTimePacer(1d / FRAMERATE);
        while (!window.shouldClose()) {
            renderer.prepare();
            renderer.render(vampy, vampyShader, camera.getCombined());

            pacer.nextFrame();
        }
    }

    private void terminate() {
        window.close();
        window.dispose();
        Loaders.disposeDefaultLoaders();
    }

    public static void main(String[] args) {
        new Game();
    }

}
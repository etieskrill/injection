package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.DirectionalLight;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;

import static org.etieskrill.engine.input.InputBinding.Trigger.PRESSED;

public class Game {

    private static final int FRAMERATE = 60;

    private Window window;
    private final Renderer renderer = new Renderer();

    private Camera camera;
    private Model vampy;
    private Shaders.StaticShader vampyShader;
    private Model[] cubes;
    private DirectionalLight globalLight;

    private final KeyInputManager controls = Input.of(
            Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(this::terminate),
            Input.bind(Keys.W).on(PRESSED).to(delta -> camera.translate(new Vector3f(0, 0, delta.floatValue()))),
            Input.bind(Keys.S).on(PRESSED).to(delta -> camera.translate(new Vector3f(0, 0, -delta.floatValue()))),
            Input.bind(Keys.A).on(PRESSED).to(delta -> camera.translate(new Vector3f(-delta.floatValue(), 0, 0))),
            Input.bind(Keys.D).on(PRESSED).to(delta -> camera.translate(new Vector3f(delta.floatValue(), 0, 0)))
    );

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
                .setSamples(4)
                .build();

        this.camera = new PerspectiveCamera(window.getSize().toVec()).setOrientation(0, 0, 0);

        this.vampy = Loaders.ModelLoader.get().load("vampy", () -> new Model.Builder("vampire_hip_hop.fbx").disableCulling().build());
        this.vampy.getTransform()
                .setPosition(new Vector3f(2.5f, -1.5f, 0))
                .setScale(.01f)
                .setRotation((float) Math.toRadians(-90), new Vector3f(0, 1, 0));
        this.vampyShader = (Shaders.StaticShader) Loaders.ShaderLoader.get().load("vampyShader", Shaders::getStandardShader);

        this.cubes = new Model[4];
        for (int i = 0; i < cubes.length; i++)
            cubes[i] = Loaders.ModelLoader.get().load("cube", () -> new Model.Builder("cube.obj").disableCulling().build());
        cubes[0].getTransform().setPosition(new Vector3f(5, 0, 0));
        cubes[1].getTransform().setPosition(new Vector3f(-5, 0, 0));
        cubes[2].getTransform().setPosition(new Vector3f(0, 0, 5));
        cubes[3].getTransform().setPosition(new Vector3f(0, 0, -5));

        globalLight = new DirectionalLight(new Vector3f(1, 1, 1), new Vector3f(2), new Vector3f(2), new Vector3f(2));
    }

    private void loop() {
        LoopPacer pacer = new SystemNanoTimePacer(1d / FRAMERATE);
        pacer.start();
        while (!window.shouldClose()) {
            renderer.prepare();

            vampy.getTransform().setRotation((float) (vampy.getTransform().getRotation() + pacer.getDeltaTimeSeconds()), vampy.getTransform().getRotationAxis());

            vampyShader.setGlobalLights(globalLight);

            renderer.render(vampy, vampyShader, camera.getCombined());
//            for (int i = 0; i < cubes.length; i++) renderer.render(cubes[i], vampyShader, camera.getCombined());

            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }

        window.dispose();
    }

    private void terminate() {
        window.close();
        Loaders.disposeDefaultLoaders();
    }

    public static void main(String[] args) {
        new Game();
    }

}
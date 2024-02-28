package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.loader.Loader;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.input.InputBinding.Trigger.ON_PRESS;
import static org.etieskrill.engine.input.InputBinding.Trigger.PRESSED;
import static org.joml.Math.toRadians;

public class Game {

    private static final int FRAMERATE = 60;

    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private Window window;
    private final Renderer renderer = new Renderer();

    private LoopPacer pacer;

    private Vector2d prevMousePosition;

    private Camera camera;
    private Model vampy;
    private AnimationShader vampyShader;
    private Model[] cubes;
    private DirectionalLight globalLight;

    private Animator vampyAnimator;

    private int currentAnimation = 0;
    private List<Animation> vampyAnimations = new ArrayList<>();
    private Label animationSelector;

    private int boneSelector = 4;
    private boolean showBoneWeights = false;

    private final KeyInputManager controls = Input.of(
            Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(this::terminate),
            Input.bind(Keys.W).on(PRESSED).to(delta -> camera.translate(new Vector3f(0, 0, delta.floatValue()))),
            Input.bind(Keys.S).on(PRESSED).to(delta -> camera.translate(new Vector3f(0, 0, -delta.floatValue()))),
            Input.bind(Keys.A).on(PRESSED).to(delta -> camera.translate(new Vector3f(-delta.floatValue(), 0, 0))),
            Input.bind(Keys.D).on(PRESSED).to(delta -> camera.translate(new Vector3f(delta.floatValue(), 0, 0))),
            Input.bind(Keys.SPACE).on(PRESSED).to(delta -> camera.translate(new Vector3f(0, -delta.floatValue(), 0))),
            Input.bind(Keys.SHIFT).on(PRESSED).to(delta -> camera.translate(new Vector3f(0, delta.floatValue(), 0))),
            Input.bind(Keys.Q).on(ON_PRESS).to(() -> {
                vampyAnimator.switchPlaying();
                logger.info("Vampy animation is {}", vampyAnimator.isPlaying() ? "playing" : "stopped");
            }),
            Input.bind(Keys.E).on(ON_PRESS).to(() -> {
                currentAnimation = ++currentAnimation % (vampyAnimations.size());
                vampyAnimator = new Animator(vampyAnimations.get(currentAnimation), vampy);
                logger.info("Switching to animation {}, '{}'", currentAnimation, vampyAnimations.get(currentAnimation).getName());
            }),
            Input.bind(Keys.R).on(ON_PRESS).to(() -> {
                boneSelector = ++boneSelector % 5;
                vampyShader.setShowBoneSelector(boneSelector);
            }),
            Input.bind(Keys.F).on(ON_PRESS).to(() -> {
                showBoneWeights = !showBoneWeights;
                vampyShader.setShowBoneWeights(showBoneWeights);
            })
    );

    Game() {
        init();
        loop();
        terminate();
    }

    private void init() {
        window = new Window.Builder()
                .setTitle("Animeshun yeeees")
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(FRAMERATE)
                .setInputManager(controls)
                .setSamples(4)
                .build();

        camera = new PerspectiveCamera(window.getSize().toVec()).setOrientation(0, 0, 0);

        vampy = Loaders.ModelLoader.get().load("vampy", () -> new Model.Builder("mixamo_walk_forward_skinned_vampire.dae").disableCulling().build());
        vampy.getInitialTransform()
                .setPosition(new Vector3f(2.5f, -1f, 0f))
                .applyRotation(quat -> quat.rotateY(toRadians(-90)))
                .setScale(.01f);
//        this.vampy.getTransform()
//                .setPosition(new Vector3f(2.5f, -1f, 0))
//                .applyRotation(quat -> quat
//                        .rotationY((float) Math.toRadians(-90))
//                        .rotateX((float) Math.toRadians(90)))
//                .setScale(.01f)
//        ;

        vampyAnimations.add(vampy.getAnimations().getFirst());

        List<Animation> orcIdle = Loader.loadModelAnimations("mixamo_orc_idle.dae", vampy);
        vampyAnimations.add(0, orcIdle.getFirst());

        //TODO animation blending
        vampyAnimator = new Animator(vampyAnimations.getFirst(), vampy);

        vampyShader = (AnimationShader) Loaders.ShaderLoader.get().load("vampyShader", AnimationShader::new);
        vampyShader.setShowBoneSelector(boneSelector);
        vampyShader.setShowBoneWeights(showBoneWeights);

        cubes = new Model[4];
        for (int i = 0; i < cubes.length; i++)
            cubes[i] = Loaders.ModelLoader.get().load("cube", () -> new Model.Builder("cube.obj").disableCulling().build());
        cubes[0].getTransform().setPosition(new Vector3f(5, 0, 0));
        cubes[1].getTransform().setPosition(new Vector3f(-5, 0, 0));
        cubes[2].getTransform().setPosition(new Vector3f(0, 0, 5));
        cubes[3].getTransform().setPosition(new Vector3f(0, 0, -5));

        globalLight = new DirectionalLight(new Vector3f(1, 1, 1), new Vector3f(2), new Vector3f(2), new Vector3f(2));

        pacer = new SystemNanoTimePacer(1d / FRAMERATE);

        window.getCursor().disable();
        prevMousePosition = window.getCursor().getPosition();
        final double sensitivity = .05;
        GLFW.glfwSetCursorPosCallback(window.getID(), (window, xpos, ypos) -> {
            camera.orient(
                    -sensitivity * (prevMousePosition.y() - ypos),
                    sensitivity * (prevMousePosition.x() - xpos), 0);
            prevMousePosition.set(xpos, ypos);
        });
    }

    private void loop() {
        pacer.start();
        while (!window.shouldClose()) {
            renderer.prepare();

//            float newScale = (float) Math.sin(pacer.getTime() * 1.5) * 0.25f + 1;
//            vampy.getTransform()
//                    .setScale(newScale)
//                    .applyRotation(rotation -> rotation.rotateY((float) pacer.getDeltaTimeSeconds()));

            vampyAnimator.update(pacer.getDeltaTimeSeconds());

            vampyShader.setBoneMatrices(vampyAnimator.getBoneMatrices());
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
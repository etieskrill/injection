package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.engine.input.InputBinding.Trigger.ON_PRESS;
import static org.etieskrill.engine.input.InputBinding.Trigger.PRESSED;

public class Game {

    private static final int FRAMERATE = 60;

    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private Window window;
    private final Renderer renderer = new Renderer();

    private Camera camera;
    private Model vampy;
    private AnimationShader vampyShader;
    private Model[] cubes;
    private DirectionalLight globalLight;

    private Animator vampyAnimator;

    private int currentAnimation;
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
                this.currentAnimation = ++currentAnimation % (vampy.getAnimations().size() - 1);
                this.vampyAnimator = new Animator(vampy.getAnimations().get(currentAnimation), vampy);
                logger.info("Switching to animation {}, '{}'", currentAnimation, vampy.getAnimations().get(currentAnimation).getName());
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
        this.window = new Window.Builder()
                .setTitle("Animeshun yeeees")
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(FRAMERATE)
                .setInputManager(controls)
                .setSamples(4)
                .build();

        this.camera = new PerspectiveCamera(window.getSize().toVec()).setOrientation(0, 0, 0);

        this.vampy = Loaders.ModelLoader.get().load("vampy", () -> new Model.Builder("vampire_hip_hop.glb").disableCulling().build());
//        this.vampy.getInitialTransform()
//                .setScale(.01f)
//                .setScale(100f)
//                .getRotation().rotationX((float) Math.toRadians(-90));
//                .getRotation().rotationY((float) Math.toRadians(-90));
        this.vampy.getTransform()
                .setPosition(new Vector3f(2.5f, -1.5f, 0))
                .applyRotation(quat -> quat
                        .rotationY((float) Math.toRadians(-90))
                        .rotateX((float) Math.toRadians(90)))
                .setScale(.01f)
//                .applyRotation(quat -> quat
//                                .rotationX((float) Math.toRadians(-90))
//                                .rotateY((float) Math.toRadians(90))
//                                .rotateZ((float) Math.toRadians(-90)))
        ;
        this.vampyShader = (AnimationShader) Loaders.ShaderLoader.get().load("vampyShader", AnimationShader::new);
        vampyShader.setShowBoneSelector(boneSelector);
        vampyShader.setShowBoneWeights(showBoneWeights);

//        System.out.println("animations available: " + vampy.getAnimations().stream().map(Animation::getName).collect(Collectors.joining(", ")));

        this.vampyAnimator = new Animator(vampy.getAnimations().get(0), vampy);

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

//            float newScale = (float) Math.sin(pacer.getTime() * 1.5) * 0.25f + 1;
//            vampy.getTransform()
//                    .setScale(newScale)
//                    .applyRotation(rotation -> rotation.rotateY((float) pacer.getDeltaTimeSeconds()));

            vampyAnimator.update(pacer.getDeltaTimeSeconds() * 0.5);
//            System.out.println(Arrays.toString(vampyAnimator.getBoneMatrices().get(20).get(new float[16])));
//            System.out.println("\n\n");

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
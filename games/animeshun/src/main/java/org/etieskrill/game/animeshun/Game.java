package org.etieskrill.game.animeshun;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.OrthographicCamera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.loader.Loader;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
    private Model cube;
    private DirectionalLight globalLight;

    private Animator vampyAnimator;

    private int currentAnimation = 0;
    private final List<Animator> vampyAnimators = new ArrayList<>();

    private Scene scene;
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
                currentAnimation = ++currentAnimation % (vampyAnimators.size());
                vampyAnimator = vampyAnimators.get(currentAnimation);
                animationSelector.setText("Current animation " + (currentAnimation + 1) + " of " + vampyAnimators.size() + ": " + vampyAnimators.get(currentAnimation).getAnimation().getName());
                logger.info("Switching to animation {}, '{}'", currentAnimation, vampyAnimators.get(currentAnimation).getAnimation().getName());
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
                .applyRotation(quat -> quat.rotateY(toRadians(-90)));

        vampyAnimators.add(new Animator(vampy.getAnimations().getFirst(), vampy));

        List<Animation> orcIdle = Loader.loadModelAnimations("mixamo_orc_idle.dae", vampy);
        vampyAnimators.add(0, new Animator(orcIdle.getFirst(), vampy));

        List<Animation> running = Loader.loadModelAnimations("mixamo_running.dae", vampy);
        Animator vampyRunningAnimator = new Animator(running.getFirst(), vampy);
        vampyRunningAnimator.setPlaybackSpeed(.85);
        vampyAnimators.add(vampyRunningAnimator);

        //Different formats sport different conventions or restrictions for the global up direction, one could either
        // - try to deduce up (and scale for that matter) from the root node of a scene, which is not difficult at all
        // - or at least stay consistent with file formats within the same bloody model
        List<Animation> hipHopDance = Loader.loadModelAnimations("vampire_hip_hop.glb", vampy);
        Transform vampyHipHopTransform = Transform.fromMatrix4f(new Matrix4f().m11(0).m12(-1).m21(1).m22(0).invert());
        vampyAnimators.add(new Animator(hipHopDance.get(2), vampy, vampyHipHopTransform));

        //TODO animation blending
        vampyAnimator = vampyAnimators.getFirst();

        vampyShader = (AnimationShader) Loaders.ShaderLoader.get().load("vampyShader", AnimationShader::new);
        vampyShader.setShowBoneSelector(boneSelector);
        vampyShader.setShowBoneWeights(showBoneWeights);

        cube = Loaders.ModelLoader.get().load("cube", () -> new Model.Builder("cube.obj").disableCulling().build());
        cube.getTransform().setScale(10).setPosition(new Vector3f(2, -6, 0));

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

        animationSelector = new Label(
                "Current animation " + (currentAnimation + 1) + " of " + vampyAnimators.size() + ": " + vampyAnimators.get(currentAnimation).getAnimation().getName(),
                Fonts.getDefault(36)
        );
        scene = new Scene(
                new Batch(renderer).setShader(Shaders.getTextShader()),
                new Container(
                        animationSelector.setAlignment(Node.Alignment.TOP_RIGHT)
                                .setMargin(new Vector4f(10))
                ),
                new OrthographicCamera(window.getSize().toVec()).setPosition(new Vector3f(window.getSize().toVec().mul(.5f), 0))
        );
        window.setScene(scene);
    }

    private void loop() {
        pacer.start();
        while (!window.shouldClose()) {
            renderer.prepare();

            vampyAnimator.update(pacer.getDeltaTimeSeconds());

            vampyShader.setBoneMatrices(vampyAnimator.getBoneMatrices());
            vampyShader.setGlobalLights(globalLight);

            renderer.render(vampy, vampyShader, camera.getCombined());
            renderer.render(cube, vampyShader, camera.getCombined());

            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }

        window.dispose();
    }

    private void terminate() {
        window.close();
        Loaders.disposeDefaultLoaders();
        TrueTypeFont.disposeLibrary();
    }

    public static void main(String[] args) {
        new Game();
    }

}
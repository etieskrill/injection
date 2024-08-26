package org.etieskrill.game.animeshun;

import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.animation.NodeFilter;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.shader.impl.AnimationShader;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.FixedArrayDeque;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.OptionalDouble;

import static org.etieskrill.engine.graphics.animation.AnimationMixer.AnimationBlendMode.OVERRIDING;
import static org.etieskrill.engine.graphics.model.loader.Loader.loadModelAnimations;
import static org.etieskrill.engine.input.InputBinding.Trigger.ON_PRESS;
import static org.etieskrill.engine.input.InputBinding.Trigger.PRESSED;
import static org.joml.Math.toRadians;

public class Game {

    private static final int FRAMERATE = 60;

    private static final Vector3fc WORLD_UP = new Vector3f(0, -1, 0);

    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private Window window;
    private final Renderer renderer = new GLRenderer();

    private LoopPacer pacer;

    private Camera camera;

    private Model vampy;
    private Model vampyBB;

    private Vector3f vampyPosDelta;
    private AnimationShader vampyShader;

    private Model cube;

    private DirectionalLight globalLight;

    private Animator vampyAnimator;

    private static final int VAMPY_ANIMATION_IDLE = 0;
    private static final int VAMPY_ANIMATION_WALKING = 1;
    private static final int VAMPY_ANIMATION_WALKING_LEFT = 2;
    private static final int VAMPY_ANIMATION_WALKING_RIGHT = 3;
    private static final int VAMPY_ANIMATION_WALKING_BACKWARD = 4;
    private static final int VAMPY_ANIMATION_RUNNING = 5;
    private static final int VAMPY_ANIMATION_WAVING = 6;
    private static final int VAMPY_ANIMATION_DANCING = 7;

    private boolean thirdPerson = false;
    private float perspectiveTransition = 0;

    private boolean waving = false;
    private float wavingTransition = 0;

    private int boneSelector = 4;
    private boolean showBoneWeights = false;

    private final KeyInputManager controls = Input.of(
            Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(this::terminate),
            Input.bind(Keys.W).on(PRESSED).to(() -> vampyPosDelta.add(0, 0, 1)),
            Input.bind(Keys.S).on(PRESSED).to(() -> vampyPosDelta.add(0, 0, -1)),
            Input.bind(Keys.A).on(PRESSED).to(() -> vampyPosDelta.add(1, 0, 0)),
            Input.bind(Keys.D).on(PRESSED).to(() -> vampyPosDelta.add(-1, 0, 0)),
//            Input.bind(Keys.SPACE).on(PRESSED).to(delta -> vampy.getTransform().translate(new Vector3f(0, -delta.floatValue(), 0))),
//            Input.bind(Keys.SHIFT).on(PRESSED).to(delta -> vampy.getTransform().translate(new Vector3f(0, delta.floatValue(), 0))),
            Input.bind(Keys.Q).on(ON_PRESS).to(() -> {
                logger.info(waving ? "Vampy stopped waving" : "Vampy started waving");
                waving = !waving;
            }),
            Input.bind(Keys.E).on(ON_PRESS).to(() -> {
                thirdPerson = !thirdPerson;
                logger.info("View set to {} person", thirdPerson ? "3rd" : "1st");
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
                .setKeyInputHandlers(controls)
                .setSamples(4)
                .build();

        GLUtils.addDebugLogging();

        camera = new PerspectiveCamera(window.getSize().toVec()).setOrientation(0, 0, 0);
        window.addCursorInputs(new CursorCameraController(camera));

        vampy = Loaders.ModelLoader.get().load("vampy", () -> new Model.Builder("mixamo_walk_forward_skinned_vampire.dae").disableCulling().build());
        vampy.getInitialTransform()
                .setPosition(new Vector3f(2.5f, -1f, 0f))
                .applyRotation(quat -> quat.rotateY(toRadians(90)));
        vampyBB = Loaders.ModelLoader.get().load("vampyBB", () -> Model.ofFile("box.obj"));
        vampyBB.getInitialTransform()
                .set(vampy.getInitialTransform())
                .setScale(new Vector3f(vampy.getBoundingBox().getSize()).mul(.01f));

        vampyPosDelta = new Vector3f();

        Node rightArmNode = vampy.getNodes().stream()
                .filter(node -> node.getName().equals("mixamorig_RightShoulder"))
                .findAny()
                .orElseThrow();

        vampyAnimator = new Animator(vampy)
                .add(loadModelAnimations("mixamo_orc_idle.dae", vampy).getFirst())
                .addNormalisedGroup(.8, animations -> animations
                        .add(loadModelAnimations("mixamo_walking_forward.dae", vampy).getFirst())
                        .add(loadModelAnimations("mixamo_left_strafe_walking.dae", vampy).getFirst())
                        .add(loadModelAnimations("mixamo_right_strafe_walking.dae", vampy).getFirst())
                        .add(loadModelAnimations("mixamo_walking_backwards.dae", vampy).getFirst(), layer -> layer.playbackSpeed(1.1))
                )
                .add(loadModelAnimations("mixamo_running.dae", vampy).getFirst(), layer -> layer.setPlaybackSpeed(.7))
                .add(loadModelAnimations("mixamo_waving.dae", vampy).getFirst(), layer -> layer.blendMode(OVERRIDING).filter(NodeFilter.tree(rightArmNode)))
                .add(loadModelAnimations("mixamo_hip_hop_dancing.dae", vampy).getFirst(), layer -> layer.enabled(false))
        ;

        vampyShader = (AnimationShader) Loaders.ShaderLoader.get().load("vampyShader", AnimationShader::new);
        vampyShader.setShowBoneSelector(boneSelector);
        vampyShader.setShowBoneWeights(showBoneWeights);

        cube = Loaders.ModelLoader.get().load("cube", () -> new Model.Builder("cube.obj").disableCulling().build());
        cube.getTransform().setScale(10).setPosition(new Vector3f(2, -6, 0));

        globalLight = new DirectionalLight(new Vector3f(1, -1, 1), new Vector3f(2), new Vector3f(2), new Vector3f(2));

        pacer = new SystemNanoTimePacer(1d / FRAMERATE);

        window.getCursor().disable();
        window.setScene(new DebugOverlay((GLRenderer) renderer, pacer, window.getSize().toVec()));

        GLUtils.removeDebugLogging();
    }

    private void loop() {
        vampyAnimator.play();

        long cpuTime;
        ArrayDeque<Double> cpuTimes = new FixedArrayDeque<>(FRAMERATE);

        float walkingFactor = 0, runningFactor = 0;

        final Vector3f currentPosition = new Vector3f(), lastPosition = new Vector3f(),
                velocity = new Vector3f(), acceleration = new Vector3f();
        double lastDelta = 0;

        float forwardFactor = 0, leftFactor = 0, rightFactor = 0, backFactor = 0;

        //Vampy head bind position for first person camera snapping
        final Node vampyHeadNode = vampy.getNodes().stream().filter(node -> node.getName().equals("mixamorig_Head")).findAny().get();
        final Vector3fc vampyHeadBindPosition = vampyHeadNode
                .getHierarchyTransform(1) //ignore scene root scaling
                .getPosition();

        pacer.start();
        while (!window.shouldClose()) {
            double delta = pacer.getDeltaTimeSeconds();

            OptionalDouble avgCpuTime = cpuTimes.stream().mapToDouble(value -> value).average();
            ((DebugOverlay) window.getScene()).setCpuTime(avgCpuTime.orElse(0));
            long time = System.nanoTime();

            if (!vampyPosDelta.equals(0, 0, 0)) {
                acceleration.set(vampyPosDelta);
                acceleration.y = 0;
                acceleration.rotateY((float) toRadians(-camera.getYaw() + 90));
                acceleration.normalize();
                acceleration.mul(controls.isPressed(Keys.W) && controls.isPressed(Keys.SHIFT)
                        && !controls.isPressed(Keys.A) && !controls.isPressed(Keys.S)
                        && !controls.isPressed(Keys.D) ? 18 : 9);
            } else {
                acceleration.zero();
            }

            //time-corrected verlet with second order taylor derivation + universal friction: p_n+1 = p_n + (1 - f) * (p_n - p_n-1) * (dt_n / dt_n-1) + a_n * dt_n * ((dt_n + dt_n-1) / 2)
            final float friction = .075f;
            currentPosition.set(vampy.getTransform().getPosition());
            final float correctedDelta = (float) (delta / lastDelta);
            vampy.getTransform().getPosition()
                    .add(velocity.set(vampy.getTransform().getPosition())
                            .sub(lastPosition)
                            .mul(Float.isFinite(correctedDelta) ? correctedDelta : 1)
                            .mul(1 - friction))
                    .add(acceleration.mul((float) (delta * ((delta + lastDelta) / 2))));
            lastPosition.set(currentPosition);
            vampyPosDelta.zero();

            vampy.getTransform().applyRotation(quat -> quat.rotationY((float) toRadians(-camera.getYaw())));

            float diff = (controls.isPressed(Keys.W)
                    || controls.isPressed(Keys.A)
                    || controls.isPressed(Keys.D)
                    || controls.isPressed(Keys.S)
            ) && !controls.isPressed(Keys.SHIFT) ? 1 - walkingFactor : -walkingFactor;
            diff *= (float) delta * 5;
            walkingFactor += diff;

            diff = (controls.isPressed(Keys.W)
                    || controls.isPressed(Keys.A)
                    || controls.isPressed(Keys.D)
                    || controls.isPressed(Keys.S)
            ) && controls.isPressed(Keys.SHIFT) ? 1 - runningFactor : -runningFactor;
            diff *= (float) delta * 5;
            runningFactor += diff;

            diff = controls.isPressed(Keys.W) ? 1 - forwardFactor : -forwardFactor;
            diff *= (float) delta * 5;
            forwardFactor += diff;
            diff = controls.isPressed(Keys.A) ? 1 - leftFactor : -leftFactor;
            diff *= (float) delta * 5;
            leftFactor += diff;
            diff = controls.isPressed(Keys.D) ? 1 - rightFactor : -rightFactor;
            diff *= (float) delta * 5;
            rightFactor += diff;
            diff = controls.isPressed(Keys.S) ? 1 - backFactor : -backFactor;
            diff *= (float) delta * 5;
            backFactor += diff;

            float factorSum = forwardFactor + leftFactor + rightFactor + backFactor;
            if (factorSum != 0) {
                forwardFactor /= factorSum;
                leftFactor /= factorSum;
                rightFactor /= factorSum;
                backFactor /= factorSum;
            }

            diff = waving ? 1 - wavingTransition : -wavingTransition;
            diff *= (float) delta * 5;
            wavingTransition += diff;

            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_IDLE, Math.max(1f - walkingFactor - runningFactor, 0));
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING, forwardFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING_LEFT, leftFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING_RIGHT, rightFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING_BACKWARD, backFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_RUNNING, runningFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WAVING, wavingTransition);
            vampyAnimator.update(delta);

            vampyShader.setBoneMatrices(vampyAnimator.getTransformMatrices());
            vampyShader.setGlobalLight(globalLight);

            diff = thirdPerson ? 1 - perspectiveTransition : -perspectiveTransition;
            perspectiveTransition += (float) (diff * delta * 10);
            Vector3f orbitPosition = null, worldUp = null;
            { //Only do calculations for both perspectives if currently switching
                if (perspectiveTransition < 1) {
                    TransformC headTransform = vampyAnimator.getTransforms().get(vampyHeadNode.getBone().id());
                    Vector3f headAnimPosition = headTransform.getMatrix().transformPosition(vampyHeadBindPosition, new Vector3f());
                    headAnimPosition.mul(vampy.getFinalTransform().getScale());
                    Vector3f headAnimWorldPosition = vampy.getFinalTransform().getMatrix().transformPosition(headAnimPosition);

                    orbitPosition = new Vector3f()
                            .add(headAnimWorldPosition)
                            .add(0, .22f, 0);
                    worldUp = vampy.getFinalTransform().getRotation().transform(headTransform.getRotation().transform(new Vector3f(WORLD_UP)));
                }
                if (perspectiveTransition > 0) {
                    Vector3f tpOrbitPosition = new Vector3f(vampy.getFinalTransform().getPosition())
                            .add(0, 1.75f, 0)
                            .sub(camera.getDirection().mul(1.75f));
                    Vector3f tpWorldUp = new Vector3f(WORLD_UP);

                    if (orbitPosition != null) orbitPosition.lerp(tpOrbitPosition, perspectiveTransition);
                    else orbitPosition = tpOrbitPosition;
                    if (worldUp != null) worldUp.lerp(tpWorldUp, perspectiveTransition);
                    else worldUp = tpWorldUp;
                }
            }
            camera.setPosition(orbitPosition);
            camera.setWorldUp(worldUp);

            renderer.prepare();

            vampyShader.setViewPosition(camera.getPosition());
            renderer.render(vampy.getTransform(), vampy, vampyShader, camera);
//            vampyBB.getTransform().set(vampy.getTransform());
//            renderer.renderWireframe(vampyBB, vampyShader, camera.getCombined());
//            renderer.renderBox(vampy.getTransform().getPosition(), vampy.getBoundingBox().getSize().mul(.01f), vampyShader, camera.getCombined());
            renderer.render(vampy.getTransform(), cube, vampyShader, camera);

            window.update(delta);

            cpuTime = System.nanoTime() - time;
            cpuTimes.push(cpuTime / 1000000d);

            lastDelta = delta;

            pacer.nextFrame();

            interrupt(window);
        }

        window.dispose();
    }

    @VisibleForTesting
    void interrupt(Window window) {
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
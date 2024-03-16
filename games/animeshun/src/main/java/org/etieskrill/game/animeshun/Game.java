package org.etieskrill.game.animeshun;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.animation.NodeFilter;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.graphics.model.loader.Loader;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.input.CursorCameraController;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.FixedArrayDeque;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.List;
import java.util.OptionalDouble;

import static org.etieskrill.engine.input.InputBinding.Trigger.ON_PRESS;
import static org.etieskrill.engine.input.InputBinding.Trigger.PRESSED;
import static org.joml.Math.toRadians;

public class Game {

    private static final int FRAMERATE = 60;

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

    private int boneSelector = 4;
    private boolean showBoneWeights = false;

    private final KeyInputManager controls = Input.of(
            Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(this::terminate),
            Input.bind(Keys.W).on(PRESSED).to(() -> vampyPosDelta.add(0, 0, 1)),
            Input.bind(Keys.S).on(PRESSED).to(() -> vampyPosDelta.add(0, 0, -1)),
            Input.bind(Keys.A).on(PRESSED).to(() -> vampyPosDelta.add(-1, 0, 0)),
            Input.bind(Keys.D).on(PRESSED).to(() -> vampyPosDelta.add(1, 0, 0)),
//            Input.bind(Keys.SPACE).on(PRESSED).to(delta -> vampy.getTransform().translate(new Vector3f(0, -delta.floatValue(), 0))),
//            Input.bind(Keys.SHIFT).on(PRESSED).to(delta -> vampy.getTransform().translate(new Vector3f(0, delta.floatValue(), 0))),
            Input.bind(Keys.Q).on(ON_PRESS).to(() -> {
                boolean waving = vampyAnimator.getAnimationMixer().isEnabled(VAMPY_ANIMATION_WAVING);
                vampyAnimator.getAnimationMixer().setEnabled(VAMPY_ANIMATION_WAVING, !waving);
                logger.info(waving ? "Vampy stopped waving" : "Vampy started waving");
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
                .setKeyInputHandler(controls)
                .setSamples(4)
                .build();

        GLUtils.addDebugLogging();

        camera = new PerspectiveCamera(window.getSize().toVec()).setOrientation(0, 0, 0);
        window.setCursorInputs(new CursorCameraController(camera));

        vampy = Loaders.ModelLoader.get().load("vampy", () -> new Model.Builder("mixamo_walk_forward_skinned_vampire.dae").disableCulling().build());
        vampy.getInitialTransform()
                .setPosition(new Vector3f(2.5f, -1f, 0f))
                .applyRotation(quat -> quat.rotateY(toRadians(-90)));
        vampyBB = Loaders.ModelLoader.get().load("vampyBB", () -> Model.ofFile("box.obj"));
        vampyBB.getInitialTransform()
                .set(vampy.getInitialTransform())
                .setScale(vampy.getBoundingBox().getSize().mul(.01f));

        vampyPosDelta = new Vector3f();

        //Different formats sport different conventions or restrictions for the global up direction, one could either
        // - try to deduce up (and scale for that matter) from the root node of a scene, which is not difficult at all
        // - or at least stay consistent with file formats within the same bloody model
        List<Animation> hipHopDance = Loader.loadModelAnimations("vampire_hip_hop.glb", vampy); //TODO -figure out why adding this animation breaks the animator- base transform is not respected in mixer
        Transform vampyHipHopTransform = Transform.fromMatrix4f(new Matrix4f().m11(0).m12(-1).m21(1).m22(0).invert());
        hipHopDance.getFirst().setBaseTransform(vampyHipHopTransform);

        Node rightArmNode = vampy.getNodes().stream()
                .filter(node -> node.getName().equals("mixamorig_RightShoulder"))
                .findAny()
                .orElseThrow();

        vampyAnimator = new Animator(vampy)
                .add(Loader.loadModelAnimations("mixamo_orc_idle.dae", vampy).getFirst())
                .addNormalisedGroup(.8, animations -> animations
                        .add(Loader.loadModelAnimations("mixamo_walking_forward.dae", vampy).getFirst())
                        .add(Loader.loadModelAnimations("mixamo_left_strafe_walking.dae", vampy).getFirst())
                        .add(Loader.loadModelAnimations("mixamo_right_strafe_walking.dae", vampy).getFirst())
                        .add(Loader.loadModelAnimations("mixamo_walking_backwards.dae", vampy).getFirst(), layer -> layer.playbackSpeed(1.1))
                )
                .add(Loader.loadModelAnimations("mixamo_running.dae", vampy).getFirst(), layer -> layer.setPlaybackSpeed(.7))
                .add(Loader.loadModelAnimations("mixamo_waving.dae", vampy).getFirst(), layer -> layer.filter(NodeFilter.tree(rightArmNode)).enabled(false))
                .add(Loader.loadModelAnimations("mixamo_hip_hop_dancing.dae", vampy).getFirst(), layer -> layer.enabled(false))
        ;

        for (int i = VAMPY_ANIMATION_WALKING; i <= VAMPY_ANIMATION_WALKING_BACKWARD; i++) {
            System.out.println(vampyAnimator.getAnimationProviders().get(i).getPlaybackSpeed());
            System.out.println(vampyAnimator.getAnimationProviders().get(i).getAnimation().getDuration());
        }

        vampyShader = (AnimationShader) Loaders.ShaderLoader.get().load("vampyShader", AnimationShader::new);
        vampyShader.setShowBoneSelector(boneSelector);
        vampyShader.setShowBoneWeights(showBoneWeights);

        cube = Loaders.ModelLoader.get().load("cube", () -> new Model.Builder("cube.obj").disableCulling().build());
        cube.getTransform().setScale(10).setPosition(new Vector3f(2, -6, 0));

        globalLight = new DirectionalLight(new Vector3f(1, 1, 1), new Vector3f(2), new Vector3f(2), new Vector3f(2));

        pacer = new SystemNanoTimePacer(1d / FRAMERATE);

        window.getCursor().disable();
        window.setScene(new DebugOverlay(renderer, pacer, window.getSize().toVec()));

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

        pacer.start();
        while (!window.shouldClose()) {
            double delta = pacer.getDeltaTimeSeconds();

            OptionalDouble avgCpuTime = cpuTimes.stream().mapToDouble(value -> value).average();
            ((DebugOverlay) window.getScene()).setCpuTime(avgCpuTime.orElse(0));
            long time = System.nanoTime();

            if (!vampyPosDelta.equals(0, 0, 0)) {
                acceleration.set(camera.relativeTranslation(vampyPosDelta));
                acceleration.y = 0;
                acceleration.normalize();
                acceleration.mul(controls.isPressed(Keys.W) && controls.isPressed(Keys.SHIFT) ? 18 : 9);
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

            vampy.getTransform().applyRotation(quat -> quat.rotationY((float) toRadians(-camera.getYaw() + 180)));

            renderer.prepare();

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

            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_IDLE, Math.max(1f - walkingFactor - runningFactor, 0));
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING, forwardFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING_LEFT, leftFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING_RIGHT, rightFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_WALKING_BACKWARD, backFactor * walkingFactor);
            vampyAnimator.getAnimationMixer().setWeight(VAMPY_ANIMATION_RUNNING, runningFactor);
            vampyAnimator.update(delta);

            vampyShader.setBoneMatrices(vampyAnimator.getTransformMatrices());
            vampyShader.setGlobalLight(globalLight);

            Node head = vampy.getNodes().stream().filter(node -> node.getName().equals("mixamorig_Head")).findAny().get();
//            List<Node> tree = new ArrayList<>(List.of(head));
//            while ((head = head.getParent()) != null)
//                tree.add(head);
            TransformC headTransform = vampyAnimator.getTransforms().get(head.getBone().id());//new Transform();
//            tree.reversed().forEach(node -> {
//                if (node.getBone() == null) return;
//                headTransform.apply(vampyAnimator.getTransforms().get(node.getBone().id()));
//            });

            Vector3f orbitPos = new Vector3f(/*vampy.getTransform().getPosition()*/)
                    .add(2.5f, 1f, -.25f)
                    .add(camera.getDirection().mul(-2.5f * .25f))
//                    .add(vampy.getTransform().getMatrix().rotateY(toRadians(270), new Matrix4f()).transformPosition(headTransform.getPosition(), new Vector3f()))
                    .add(vampy.getTransform().getMatrix().transformPosition(headTransform.getPosition(), new Vector3f()));
            camera.setPosition(orbitPos);

            renderer.render(vampy, vampyShader, camera.getCombined());
//            vampyBB.getTransform().set(vampy.getTransform());
//            renderer.renderWireframe(vampyBB, vampyShader, camera.getCombined());
//            renderer.renderBox(vampy.getTransform().getPosition(), vampy.getBoundingBox().getSize().mul(.01f), vampyShader, camera.getCombined());
            renderer.render(cube, vampyShader, camera.getCombined());

            window.update(delta);

            cpuTime = System.nanoTime() - time;
            cpuTimes.push(cpuTime / 1000000d);

            lastDelta = delta;

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
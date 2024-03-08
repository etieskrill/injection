package org.etieskrill.orbes.scene;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.input.*;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.orbes.Game;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class GameScene {

    private static final Logger logger = LoggerFactory.getLogger(GameScene.class);

    private Camera camera;

    private Model skelly, localSkellyBBModel, skellyBBModel;
    private Model cube, light;
    private PointLight pointLight;
    private PointLight[] pointLights;
    private Model[] orbs;

    private final Vector3f deltaPos = new Vector3f(0);
    private float rotation, smoothRotation;
    private float jumpTime;
    private float smoothSkellyHeight;

    public static final int NUM_ORBS = 10;
    private int orbsCollected;

    Shaders.StaticShader shader;
    Shaders.LightSourceShader lightShader;

    private KeyInputManager keyInputManager;

    public GameScene(Game game) {
        init(game);
    }

    private void init(Game game) {
        loadModels();

        shader = (Shaders.StaticShader) Loaders.ShaderLoader.get().load("standard", Shaders::getStandardShader);
        lightShader = (Shaders.LightSourceShader) Loaders.ShaderLoader.get().load("light", Shaders::getLightSourceShader);

        camera = new PerspectiveCamera(game.getWindow().getSize().toVec());
        camera.setZoom(4.81f);

        pointLight = new PointLight(light.getTransform().getPosition(),
                new Vector3f(.1f), new Vector3f(.3f), new Vector3f(.5f),
                1f, 0.03f, 0.005f);
        pointLights = new PointLight[]{pointLight};

        setupInput(game);
        setupCursor(game);
    }

    private void loadModels() {
        Loaders.ModelLoader models = Loaders.ModelLoader.get();

        cube = models.load("cube", () -> Model.ofFile("cube.obj"));
        cube.getTransform().setScale(50).setPosition(new Vector3f(0, -25, 0));
        light = models.get("cube");
        light.getTransform().setScale(0.5f).setPosition(new Vector3f(2, 5, -2));
        skelly = models.load("skelly", () -> Model.ofFile("skeleton.glb"));
        skelly.getInitialTransform().setScale(15);

        Transform localBBBaseTransform = skelly.getNodes().getFirst().getHierarchyTransform();
        Vector3f localSkellyBBSize = localBBBaseTransform.getMatrix().transformPosition(skelly.getBoundingBox().getSize());
        localSkellyBBModel = ModelFactory.box(localSkellyBBSize);
//        localSkellyBBModel.getInitialTransform().translate(
//                skelly.getBoundingBox().getCenter().mul(.5f)
//        );

        skellyBBModel = ModelFactory.box(skelly.getBoundingBox().getMax().sub(skelly.getBoundingBox().getMin(), new Vector3f()));
        skellyBBModel.getInitialTransform().setPosition(
                skelly.getBoundingBox().getCenter().mul(skelly.getTransform().getScale())
        );

        orbs = new Model[NUM_ORBS];
        Random random = new Random(69420);
        for (int i = 0; i < NUM_ORBS; i++) {
            orbs[i] = models.load("orb", () -> Model.ofFile("Sphere.obj"));
            orbs[i].getTransform()
                    .setScale(new Vector3f(0.02f))
                    .setPosition(new Vector3f(
                            random.nextFloat() * 50 - 25,
                            random.nextFloat() * 4.5f + 0.5f,
                            random.nextFloat() * 50 - 25
                    ));
        }
        Model orbBBModel = ModelFactory.box(orbs[0].getBoundingBox().getMax().sub(orbs[0].getBoundingBox().getMin(), new Vector3f()));
        orbBBModel.getInitialTransform().setPosition(
                orbBBModel.getBoundingBox().getCenter()
        );
    }

    private void setupInput(Game game) {
        keyInputManager = Input.of(
                Input.bind(Keys.ESC).to(() -> {
                    if (game.getPacer().isPaused()) game.unpause();
                    else game.pause();
                }),
                Input.bind(Keys.ESC.withMods(Keys.SHIFT)).to(() -> game.getWindow().close()),
                Input.bind(Keys.W).on(InputBinding.Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.S).to(() -> deltaPos.add(0, 0, 1)),
                Input.bind(Keys.S).on(InputBinding.Trigger.PRESSED).to(() -> deltaPos.add(0, 0, -1)),
                Input.bind(Keys.A).on(InputBinding.Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.D).to(() -> deltaPos.add(-1, 0, 0)),
                Input.bind(Keys.D).on(InputBinding.Trigger.PRESSED).to(() -> deltaPos.add(1, 0, 0))
        );
    }

    private void setupCursor(Game game) {
        game.getWindow().setCursorInputs(
                new CursorCameraController(camera, .04, 0)
                        .setUpdateCondition(() -> !game.getPacer().isPaused() && game.getStage() == Game.Stage.GAME)
        );
    }

    public void updateScene(double delta) {
        transformSkelly(delta);
        collideOrbs();
    }

    private void transformSkelly(double delta) {
        Vector3f skellyTranslate = camera
                .relativeTranslation(deltaPos)
                .mul(1, 0, 1);
        if (skellyTranslate.lengthSquared() > 0) skellyTranslate.normalize();
        skellyTranslate = skellyTranslate
                .mul((float) delta)
                .mul(5f * (keyInputManager.isPressed(Keys.CTRL) ? 1.5f : 1));
        skelly.getTransform().translate(skellyTranslate);


        if (!deltaPos.equals(0, 0, 0)) {
            rotation = (float) (Math.atan2(deltaPos.z(), deltaPos.x()) - Math.toRadians(camera.getYaw()));
            rotation %= Math.toRadians(360);
            //TODO fix shortest distance through wraparound
            //TODO include delta time
            if (Math.abs(rotation - smoothRotation) > 0.001) {
                double falloff = -0.5 * (1 / (2 * Math.abs(rotation - smoothRotation) + 0.5)) + 1;
                falloff *= 500 * delta;
                smoothRotation += Math.toRadians(rotation - smoothRotation >= 0 ? falloff : -falloff);
            }
            smoothRotation %= Math.toRadians(360);
            skelly.getTransform().getRotation().rotationY(smoothRotation);
        }
        deltaPos.set(0);

        skelly.getTransform().getPosition().x = Math.max(-25, Math.min(25, skelly.getTransform().getPosition().x())); //why not use Math#clamp? try it, smartass
        skelly.getTransform().getPosition().z = Math.max(-25, Math.min(25, skelly.getTransform().getPosition().z()));

        if (keyInputManager.isPressed(Keys.SPACE) && jumpTime == 0)
            jumpTime += 0.0001f;
        if (jumpTime != 0 && jumpTime < 1) {
            double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
            skelly.getTransform().getPosition().y = (float) jumpHeight;
            jumpTime += delta;
        } else jumpTime = 0;

        float skellyHeight;
        if (keyInputManager.isPressed(Keys.LEFT_SHIFT)) skellyHeight = .6f;
        else skellyHeight = 1;

        double falloff = -0.5 * (1 / (2 * Math.abs(skellyHeight - smoothSkellyHeight) + 0.5)) + 1;
        falloff = 5 * falloff * delta;
        smoothSkellyHeight += skellyHeight > smoothSkellyHeight ? falloff : -falloff;

        skelly.getTransform().applyScale(scale -> scale.y = smoothSkellyHeight);
    }

    private void collideOrbs() {
        for (Model orb : orbs) {
            if (!orb.isEnabled()) continue;

            Vector3f direction = new Vector3f(orb.getTransform().getPosition()).sub(skelly.getTransform().getPosition());
            if (new Vector2f(direction.x(), direction.z()).length() < 1 && direction.y() > 0 && direction.y() < 4) {
                orb.disable();
                orbsCollected++;
            }
        }
    }

    public void updateCamera() {
        Vector3f orbitPos = camera.getDirection().negate().mul(3);
        float skellyVerticalCenter = skelly.getWorldBoundingBox().getSize().y() - .5f;
        float localSkellyVertCenter = skelly.getBoundingBox().getSize().y() - .5f;
        localSkellyVertCenter = -(localSkellyVertCenter / skelly.getFinalTransform().getScale().y()) + 5;
        camera.setPosition(
                skelly.getFinalTransform().getPosition()
                        .add(orbitPos)
                        .add(0, localSkellyVertCenter, 0)
        );
    }

    public void render(Renderer renderer) {
        shader.setViewPosition(camera.getPosition());
        shader.setLights(pointLights);

        renderer.render(cube, shader, camera.getCombined());
        renderer.render(skelly, shader, camera.getCombined());

//        localSkellyBBModel.getTransform().set(skelly.getFinalTransform());
//        renderer.renderWireframe(localSkellyBBModel, shader, camera.getCombined());
//        skellyBBModel.getTransform().set(skelly.getTransform());
//        renderer.renderWireframe(skellyBBModel, shader, camera.getCombined());

        for (Model orb : orbs) {
            if (!orb.isEnabled()) continue;
            renderer.render(orb, shader, camera.getCombined());
        }

        lightShader.setLight(pointLight);
        renderer.render(light, lightShader, camera.getCombined());
    }

    public Camera getCamera() {
        return camera;
    }

    public KeyInputManager getKeyInputManager() {
        return keyInputManager;
    }

    public int getOrbsCollected() {
        return orbsCollected;
    }

    public void reset() {
        for (Model orb : orbs) orb.enable();
        orbsCollected = 0;
        skelly.getTransform().setPosition(new Vector3f(0));
    }

}

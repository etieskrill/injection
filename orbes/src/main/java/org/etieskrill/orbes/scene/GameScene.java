package org.etieskrill.orbes.scene;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.PointLight;
import org.etieskrill.engine.input.*;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.orbes.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;

public class GameScene {

    private static final Logger logger = LoggerFactory.getLogger(GameScene.class);

    private Camera camera;

    private Model skelly, skellyBBModel;
    private Model cube, light;
    private PointLight pointLight;
    private PointLight[] pointLights;
    private Model[] orbs;

    private final Vec3 deltaPos = new Vec3(0);
    private float rotation, smoothRotation;
    private float jumpTime;
    private float smoothSkellyHeight;

    public static final int NUM_ORBS = 10;
    private int orbsCollected;

    Shaders.StaticShader shader;
    Shaders.LightSourceShader lightShader;

    private KeyInputManager keyInputManager;

    private double prevCursorPosX, prevCursorPosY;

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
                new Vec3(1), new Vec3(1), new Vec3(1),
                1f, 0.03f, 0.005f);
        pointLights = new PointLight[]{pointLight};

        setupInput(game);
        setupCursor(game);
    }

    private void loadModels() {
        Loaders.ModelLoader models = Loaders.ModelLoader.get();

        cube = models.load("cube", () -> Model.ofFile("cube.obj"));
        cube.getTransform().setScale(50).setPosition(new Vec3(0, -25, 0));
        light = models.get("cube");
        light.getTransform().setScale(0.5f).setPosition(new Vec3(2, 5, -2));
        skelly = models.load("skelly", () -> Model.ofFile("skeleton.glb"));
        skelly.getTransform().setScale(15);

        skellyBBModel = ModelFactory.box(skelly.getBoundingBox().getMax().minus(skelly.getBoundingBox().getMin()));
        skellyBBModel.getTransform()
                .setInitialPosition(
                        skelly.getBoundingBox().getCenter().times(skelly.getTransform().getScale())
                );

        orbs = new Model[NUM_ORBS];
        Random random = new Random(69420);
        for (int i = 0; i < NUM_ORBS; i++) {
            orbs[i] = models.load("orb", () -> Model.ofFile("Sphere.obj"));
            orbs[i].getTransform()
                    .setScale(new Vec3(0.02))
                    .setPosition(new Vec3(
                            random.nextFloat() * 50 - 25,
                            random.nextFloat() * 4.5 + 0.5,
                            random.nextFloat() * 50 - 25
                    ));
        }
        Model orbBBModel = ModelFactory.box(orbs[0].getBoundingBox().getMax().minus(orbs[0].getBoundingBox().getMin()));
        orbBBModel.getTransform().setInitialPosition(
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
                Input.bind(Keys.W).on(InputBinding.Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.S).to(() -> deltaPos.plusAssign(0, 0, 1)),
                Input.bind(Keys.S).on(InputBinding.Trigger.PRESSED).to(() -> deltaPos.plusAssign(0, 0, -1)),
                Input.bind(Keys.A).on(InputBinding.Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.D).to(() -> deltaPos.plusAssign(-1, 0, 0)),
                Input.bind(Keys.D).on(InputBinding.Trigger.PRESSED).to(() -> deltaPos.plusAssign(1, 0, 0))
        );
    }

    private void setupCursor(Game game) {
        glfwSetCursorPosCallback(game.getWindow().getID(), ((window1, xpos, ypos) -> {
            double dx = prevCursorPosX - xpos;
            double dy = prevCursorPosY - ypos;

            double sens = 0.04;
            if (!game.getPacer().isPaused() && game.getStage() == Game.Stage.GAME)
                camera.orient(-dy * sens, dx * sens, 0);

            prevCursorPosX = xpos;
            prevCursorPosY = ypos;
        }));
    }

    public void updateScene(double delta) {
        transformSkelly(delta);
        collideOrbs();
    }

    private void transformSkelly(double delta) {
        Vec3 skellyTranslate = camera
                .relativeTranslation(deltaPos)
                .times(1, 0, 1);
        if (skellyTranslate.length2() > 0) skellyTranslate.normalizeAssign();
        skellyTranslate = skellyTranslate
                .times(delta)
                .times(5 * (keyInputManager.isPressed(Keys.CTRL) ? 1.5 : 1));
        skelly.getTransform().translate(skellyTranslate);

        if (deltaPos.anyNotEqual(0, 0.001f)) {
            rotation = (float) (Math.atan2(deltaPos.getZ(), deltaPos.getX()) - Math.toRadians(camera.getYaw()));
            rotation %= Math.toRadians(360);
            //TODO fix shortest distance through wraparound
            //TODO include delta time
            if (Math.abs(rotation - smoothRotation) > 0.001) {
                double falloff = -0.5 * (1 / (2 * Math.abs(rotation - smoothRotation) + 0.5)) + 1;
                falloff *= 500 * delta;
                smoothRotation += Math.toRadians(rotation - smoothRotation >= 0 ? falloff : -falloff);
            }
            smoothRotation %= Math.toRadians(360);
            skelly.getTransform().setRotation(
                    smoothRotation,
                    new Vec3(0, 1, 0));
        }
        deltaPos.put(0);

        skelly.getTransform().getPosition().setX(Math.max(-25, Math.min(25, skelly.getTransform().getPosition().getX()))); //why not use Math#clamp? try it, smartass
        skelly.getTransform().getPosition().setZ(Math.max(-25, Math.min(25, skelly.getTransform().getPosition().getZ())));

        if (keyInputManager.isPressed(Keys.SPACE) && jumpTime == 0)
            jumpTime += 0.0001;
        if (jumpTime != 0 && jumpTime < 1) {
            double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
            skelly.getTransform().getPosition().setY((float) jumpHeight);
            jumpTime += delta;
        } else jumpTime = 0;

        float skellyHeight;
        if (keyInputManager.isPressed(Keys.LEFT_SHIFT)) skellyHeight = 9;
        else skellyHeight = 15;

        double falloff = -0.5 * (1 / (2 * Math.abs(skellyHeight - smoothSkellyHeight) + 0.5)) + 1;
        falloff = 20 * falloff * delta;
        smoothSkellyHeight += skellyHeight > smoothSkellyHeight ? falloff : -falloff;

        skelly.getTransform().setScale(new Vec3(15, smoothSkellyHeight, 15));
    }

    private void collideOrbs() {
        for (Model orb : orbs) {
            if (!orb.isEnabled()) continue;

            Vec3 direction = orb.getTransform().getPosition().minus(skelly.getTransform().getPosition());
            if (new Vec2(direction.getX(), direction.getZ()).length() < 1 && direction.getY() > 0 && direction.getY() < 4) {
                orb.disable();
                orbsCollected++;
            }
        }
    }

    public void updateCamera() {
        Vec3 orbitPos = camera.getDirection().negate().times(3);
        camera.setPosition(orbitPos.plus(0, skelly.getWorldBoundingBox().getSize().getY() - 0.5, 0).plus(skelly.getTransform().getPosition()));
    }

    public void render(Renderer renderer) {
        shader.setViewPosition(camera.getPosition());
        shader.setLights(pointLights);

        renderer.render(cube, shader, camera.getCombined());
        renderer.render(skelly, shader, camera.getCombined());

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
        skelly.getTransform().setPosition(new Vec3(0));
    }

}

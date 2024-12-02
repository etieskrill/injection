package org.etieskrill.orbes.scene;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.entity.service.impl.PhysicsService;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.input.*;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.orbes.Game;
import org.joml.Vector3f;
import org.joml.primitives.AABBf;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;

import static org.joml.Math.*;

public class GameScene {

    private Camera camera;

    private Transform skellyTransform;
    private AABBf skellyBoundingBox;

    private Entity[] orbs;

    private final Vector3f deltaPos = new Vector3f(0);
    private float rotation, smoothRotation;
    private float jumpTime;
    private float smoothSkellyHeight;

    public static final int NUM_ORBS = 10;
    private int orbsCollected;

    private KeyInputManager keyInputManager;

    public GameScene(Game game) {
        super();
        init(game);
    }

    private void init(Game game) {
        camera = new PerspectiveCamera(game.getWindow().getSize().getVec())
                .setOrbit(true)
                .setOrbitDistance(3)
                .setZoom(4.81f);

        loadEntities(game);

        game.getEntitySystem().addService(
                new PhysicsService(new PhysicsService.NarrowCollisionSolver() {
                    @Override
                    public void solveStatic(Transform transform, Transform otherTransform, WorldSpaceAABB bb, WorldSpaceAABB otherBB, DynamicCollider collider, StaticCollider otherCollider, Entity entity, Entity otherEntity, OnGround onGround) {
                        Vector3f overlap = bb.intersection(otherBB, new AABBf()).getSize(new Vector3f());

                        int component = overlap.minComponent();
                        float overlapComponent = overlap.get(component);
                        if (overlapComponent <= 0) return;
                        if (bb.center(new Vector3f()).get(component) < otherBB.center(new Vector3f()).get(component)) {
                            overlapComponent = -overlapComponent;
                        }
                        if (component == 1 && onGround != null) {
                            onGround.setOnGround(true);
                        }
                        transform.getPosition().setComponent(component,
                                transform.getPosition().get(component) + overlapComponent);
                        collider.getPreviousPosition().setComponent(component, transform.getPosition().get(component));

                        //TODO custom per-type handler - probably in PhysicsService?
                        Arrays.stream(orbs) //FIXME puke puke puke - but giving shrek head O_o ?
                                .filter(Predicate.isEqual(entity))
                                .findAny()
                                .ifPresent(targetEntity -> {
                                    entity.getComponent(Enabled.class).setEnabled(false);
                                    orbsCollected++;
                                });
                    }

                    @Override
                    public void solveDynamic(Transform transform, Transform otherTransform, WorldSpaceAABB bb, WorldSpaceAABB otherBB, DynamicCollider collider, DynamicCollider otherCollider, Entity entity, Entity otherEntity) {
                    }
                })
        );

        setupInput(game);
        setupCursor(game);
    }

    private void loadEntities(Game game) {
        var system = game.getEntitySystem();
        var models = Loaders.ModelLoader.get();

        var cube = system.createEntity()
                .withComponent(new Transform().setScale(50).setPosition(new Vector3f(0, -25, 0)))
                .withComponent(new Drawable(models.load("cube", () -> Model.ofFile("cube.obj"))))
                .withComponent(new WorldSpaceAABB())
                .withComponent(new StaticCollider());

        var pointLight = system.createEntity(id -> {
            var transform = new Transform().setScale(0.5f).setPosition(new Vector3f(2, 5, -2));
            return new Entity(id)
                    .withComponent(transform)
                    .withComponent(new Drawable(models.load("cube", () -> Model.ofFile("cube.obj"))))
                    .withComponent(new PointLight(transform.getPosition(),
                            new Vector3f(.1f), new Vector3f(.3f), new Vector3f(.5f),
                            1f, 0.03f, 0.005f));
        });

        var skelly = system.createEntity(id -> {
            var entity = new Entity(id);
            skellyTransform = new Transform();
            var drawable = entity
                    .withComponent(skellyTransform)
                    .withComponent(new DynamicCollider())
                    .withComponent(new WorldSpaceAABB())
                    .addComponent(new Drawable(models.load("skelly",
                            () -> new Model.Builder("skeleton.glb")
                                    .setInitialTransform(new Transform().setScale(.2f))
                                    .build())));
            skellyBoundingBox = drawable.getModel().getBoundingBox();
            return entity;
        });

        orbs = new Entity[NUM_ORBS];
        Random random = new Random(69420);
        for (int i = 0; i < NUM_ORBS; i++) {
            orbs[i] = system.createEntity()
                    .withComponent(new Enabled())
                    .withComponent(new Transform()
                            .setScale(new Vector3f(0.02f))
                            .setPosition(new Vector3f(
                                    random.nextFloat() * 50 - 25,
                                    random.nextFloat() * 4.5f + 0.5f,
                                    random.nextFloat() * 50 - 25
                            )))
                    .withComponent(new Drawable(models.load("orb", () -> Model.ofFile("Sphere.obj"))))
                    .withComponent(new WorldSpaceAABB())
                    .withComponent(new StaticCollider());
        }
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
        game.getWindow().addCursorInputs(
                new CursorCameraController(camera, .04, 0)
                        .setUpdateCondition(() -> !game.getPacer().isPaused() && game.getStage() == Game.Stage.GAME)
        );
    }

    public void updateScene(double delta) {
        transformSkelly(delta);
    }

    private void transformSkelly(double delta) {
        Vector3f skellyTranslate = camera
                .relativeTranslation(deltaPos)
                .mul(1, 0, 1);
        if (skellyTranslate.lengthSquared() > 0) skellyTranslate.normalize();
        skellyTranslate = skellyTranslate
                .mul((float) delta)
                .mul(5f * (keyInputManager.isPressed(Keys.CTRL) ? 1.5f : 1));
        skellyTransform.translate(skellyTranslate);


        if (!deltaPos.equals(0, 0, 0)) {
            rotation = (float) (atan2(deltaPos.z(), deltaPos.x()) - Math.toRadians(camera.getYaw()));
            rotation %= toRadians(360);
            //TODO fix shortest distance through wraparound
            //TODO include delta time
            if (Math.abs(rotation - smoothRotation) > 0.001) {
                double falloff = -0.5 * (1 / (2 * abs(rotation - smoothRotation) + 0.5)) + 1;
                falloff *= 500 * delta;
                smoothRotation += toRadians(rotation - smoothRotation >= 0 ? falloff : -falloff);
            }
            smoothRotation %= toRadians(360);
            skellyTransform.getRotation().rotationY(smoothRotation);
        }
        deltaPos.set(0);

        skellyTransform.getPosition().x = max(-25, min(25, skellyTransform.getPosition().x())); //why not use Math#clamp? try it, smartass
        skellyTransform.getPosition().z = max(-25, min(25, skellyTransform.getPosition().z()));

        if (keyInputManager.isPressed(Keys.SPACE) && jumpTime == 0)
            jumpTime += 0.0001f;
        if (jumpTime != 0 && jumpTime < 1) {
            double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
            skellyTransform.getPosition().y = (float) jumpHeight;
            jumpTime += (float) delta;
        } else jumpTime = 0;

        float skellyHeight;
        if (keyInputManager.isPressed(Keys.LEFT_SHIFT)) skellyHeight = .6f;
        else skellyHeight = 1;

        double falloff = -0.5 * (1 / (2 * Math.abs(skellyHeight - smoothSkellyHeight) + 0.5)) + 1;
        falloff = 5 * falloff * delta;
        smoothSkellyHeight += (float) (skellyHeight > smoothSkellyHeight ? falloff : -falloff);

        skellyTransform.applyScale(scale -> scale.y = smoothSkellyHeight);
    }

    public void updateCamera() {
        float skellyVerticalCenter = skellyTransform.getScale().y() * (skellyBoundingBox.maxY - skellyBoundingBox.minY) - .5f;
        camera.setPosition(
                new Vector3f(skellyTransform.getPosition()).add(0, skellyVerticalCenter, 0)
        );
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
        for (Entity orb : orbs) {
            if (orb == null) continue;
            orb.getComponent(Enabled.class).setEnabled(true);
        }
        orbsCollected = 0;
        skellyTransform.setPosition(new Vector3f(0));
    }

}

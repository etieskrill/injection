package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.component.Acceleration;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.service.*;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCharacterTranslationController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.engine.entity.service.PhysicsService.NarrowCollisionSolver.AABB_SOLVER;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_SRGB;

public class EntityApplication extends GameApplication {

    private static final int FRAME_RATE = 60;

    static final Loaders.ModelLoader MODELS = Loaders.ModelLoader.get();

    private static final Logger logger = LoggerFactory.getLogger(EntityApplication.class);

    private Camera camera;

    private boolean light = true;
    private static final Vector3fc lightOn = new Vector3f(1);
    private static final Vector3fc lightOff = new Vector3f(0);

    private World world;

    private Transform playerTransform;
    private Acceleration playerMoveForce;

    private static final float PLAYER_WALKING_SPEED = 10;
    private static final float PLAYER_RUNNING_SPEED = 30;

    private DebugInterface debugInterface;

    public EntityApplication() {
        super(FRAME_RATE, new Window.Builder()
                .setTitle("Horde")
                .setMode(Window.WindowMode.BORDERLESS)
                .setSamples(4)
                .setVSyncEnabled(true)
                .build()
        );
    }

    @Override
    protected void init() {
        GLUtils.addDebugLogging();

        camera = new PerspectiveCamera(window.getSize().toVec());

        world = new World(entitySystem);

        glEnable(GL_FRAMEBUFFER_SRGB);

        debugInterface = new DebugInterface(window, renderer);

        entitySystem.addService(new BoundingBoxService());
        entitySystem.addService(new DirectionalShadowMappingService(renderer, new Shaders.DepthShader()));
        entitySystem.addService(new PointShadowMappingService(renderer, new Shaders.DepthCubeMapArrayShader()));
        entitySystem.addService(new RenderService(renderer, camera, window.getSize().toVec()));
        entitySystem.addService(new BoundingBoxRenderService(renderer, camera));
        entitySystem.addService(new PhysicsService(AABB_SOLVER));

        PlayerEntity player = entitySystem.createEntity(PlayerEntity::new);
        playerTransform = player.getTransform();
        playerMoveForce = player.getMoveForce();

        window.addCursorInputs(new CursorCameraController(camera));
        KeyCharacterTranslationController playerController = (KeyCharacterTranslationController)
                new KeyCharacterTranslationController((Vector3f) playerMoveForce.getForce(), camera)
                        .setSpeed(PLAYER_WALKING_SPEED);
        window.addKeyInputs(playerController);
        window.getCursor().disable();

        window.addKeyInputs(Input.of(
                Input.bind(Keys.Q).to(() -> {
                    light = !light;
                    logger.info("Turning sunlight {}", light ? "on" : "off");

                    world.getSunLight().setAmbient(light ? lightOn : lightOff);
                    world.getSunLight().setDiffuse(light ? lightOn : lightOff);
                    world.getSunLight().setSpecular(light ? lightOn : lightOff);
                }),
                Input.bind(Keys.CTRL).to(() -> {
                    if (playerController.getSpeed() == PLAYER_WALKING_SPEED)
                        playerController.setSpeed(PLAYER_RUNNING_SPEED);
                    else playerController.setSpeed(PLAYER_WALKING_SPEED);
                }),
                Input.bind(Keys.C).to(() -> {
                    player.getCollider().setPreviousPosition(playerTransform.getPosition());
                })
        ));
    }

    @Override
    protected void loop(final double delta) {
        camera.setOrientation(-45, 45, 0);
        camera.setPosition(
                new Vector3f(playerTransform.getPosition())
                        .add(0, 2, 0)
                        .sub(camera.getDirection().mul(8))
        );

        world.getSunTransform().setPosition(new Vector3f(50).add(camera.getPosition()));
        world.getCubeTransform().applyRotation(quat -> quat.rotateAxis((float) delta, 1, 1, 1));

        if (pacer.getTotalFramesElapsed() % 60 == 0) {
            logger.info("Fps: {}, cpu time: {}ms, gpu time: {}ms, gpu delay: {}ms",
                    "%4.1f".formatted(pacer.getAverageFPS()),
                    "%5.2f".formatted(getAvgCpuTime()),
                    "%5.2f".formatted(renderer.getAveragedGpuTime() / 1_000_000.0),
                    "%5.2f".formatted(renderer.getGpuDelay() / 1_000_000.0));
        }
        debugInterface.getFpsLabel().setText(String.valueOf((int) pacer.getAverageFPS()));
    }

    public static void main(String[] args) {
        new EntityApplication();
    }

}

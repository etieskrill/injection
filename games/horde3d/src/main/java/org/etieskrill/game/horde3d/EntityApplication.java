package org.etieskrill.game.horde3d;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.service.impl.*;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShader;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCharacterTranslationController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Math;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.engine.entity.service.impl.PhysicsService.NarrowCollisionSolver.AABB_SOLVER;
import static org.etieskrill.engine.input.Input.bind;
import static org.joml.Math.floor;
import static org.joml.Math.toRadians;

public class EntityApplication extends GameApplication {

    private static final int FRAME_RATE = 1000;

    static final Loaders.ModelLoader MODELS = Loaders.ModelLoader.get();

    private static final Logger logger = LoggerFactory.getLogger(EntityApplication.class);

    private Camera camera;

    private RenderService secondaryRenderService;

    private boolean light;
    private static final Vector3fc lightOnAmbient = new Vector3f(1f);
    private static final Vector3fc lightOnDiffuse = new Vector3f(5);
    private static final Vector3fc lightOnSpecular = new Vector3f(5);
    private static final Vector3fc lightOff = new Vector3f(0);

    private World world;

    private PlayerEntity player;
    private Zombie zombie;

    private DebugInterface debugInterface;

    private boolean hdrReinhardMapping;
    private float hdrExposure;

    private double previousTime;

    public EntityApplication() {
        super(Window.builder()
                .setTitle("Horde3d")
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(FRAME_RATE)
                .setSamples(4)
                .setVSyncEnabled(true)
                .build()
        );
    }

    @Override
    protected void init() {
        GLUtils.addDebugLogging();

        camera = new PerspectiveCamera(window.getSize().getVec());

        world = new World(entitySystem);

        entitySystem.addService(new BoundingBoxService());

        //TODO fix static-animated root transform
        entitySystem.addService(new DirectionalShadowMappingService(renderer));
        entitySystem.addService(new PointShadowMappingService(renderer,
//                new DepthCubeMapArrayAnimatedShader() //TODO revert
                new DepthCubeMapArrayShader()
        ));
        entitySystem.addService(new AnimationService());
        RenderService renderService = new RenderService(renderer, camera, window.getSize().getVec());
        entitySystem.addService(renderService);

        float smolFactor = 4;
        secondaryRenderService = new RenderService(renderer,
                new PerspectiveCamera(window.getSize().getVec())
                        .setPosition(new Vector3f(-10, 10, -10))
                        .setRotation(-45, -45, 0)
                        .setZoom(10f),
                new Vector2i(window.getSize().getVec()).div(smolFactor))
                .cullingCamera(camera)
                .blur(false)
                .customViewport(new Vector4i(
                        (int) (window.getSize().getWidth() * (1f - 1f / smolFactor)),
                        (int) (window.getSize().getHeight() * (1f - 1f / smolFactor)),
                        (int) (window.getSize().getWidth() * 1f / smolFactor),
                        (int) (window.getSize().getHeight() * 1f / smolFactor))
                );
        entitySystem.addService(secondaryRenderService);

        entitySystem.addService(new PhysicsService(AABB_SOLVER));
        entitySystem.addService(new SnippetsService());

        player = entitySystem.createEntity(PlayerEntity::new);

        final int numZombies = 10;
        for (int i = 0; i < numZombies; i++) {
            float angle = toRadians(((float) i / numZombies) * 360f);

            zombie = entitySystem.createEntity(Zombie::new);
            zombie.getTransform().getPosition().add(20 * Math.cos(angle), 0, 20 * Math.sin(angle));
            zombie.getCollider().setPreviousPosition(zombie.getTransform().getPosition());
        }

        window.addCursorInputs(new CursorCameraController(camera));
        KeyCharacterTranslationController playerController =
                new KeyCharacterTranslationController(player.getMoveForce().getForce(), camera);
        playerController
                .removeBindings(Keys.SPACE.getInput(), Keys.SHIFT.getInput())
                .addBindings(Input.bind(Keys.SPACE).to(player.getDashState()::trigger));
        window.addKeyInputs(playerController);
        window.getCursor().disable();

        light = true;

        hdrReinhardMapping = true;
        hdrExposure = 1;

        window.addKeyInputs(Input.of(
                bind(Keys.Q).to(() -> {
                    light = !light;
                    logger.info("Turning sunlight {}", light ? "on" : "off");

                    world.getSunLight().setAmbient(light ? lightOnAmbient : lightOff);
                    world.getSunLight().setDiffuse(light ? lightOnDiffuse : lightOff);
                    world.getSunLight().setSpecular(light ? lightOnSpecular : lightOff);
                }),
                bind(Keys.E).to(() -> {
                    hdrReinhardMapping = !hdrReinhardMapping;
                    renderService.getHdrShader().setUniform("reinhard", hdrReinhardMapping);
                }),
                bind(Keys.T).to(() -> {
                    hdrExposure += .25f;
                    renderService.getHdrShader().setUniform("exposure", hdrExposure);
                }),
                bind(Keys.G).to(() -> {
                    hdrExposure -= .25f;
                    renderService.getHdrShader().setUniform("exposure", hdrExposure);
                }),
                bind(Keys.F1).to(() -> {
                    renderService.getBoundingBoxRenderService().toggleRenderBoundingBoxes();
                })
        ));

        //FIXME loading the scene (the label font specifically) before the above stuff causes a segfault from freetype??
        debugInterface = new DebugInterface(window.getSize().getVec(), renderer, pacer);
        window.setScene(debugInterface); //FIXME WAAAAAH MOM THE TEXT RENDERERING IS BRICKED AGAIN

        GLUtils.removeDebugLogging();
    }

    @Override
    protected void loop(final double delta) {
//        player.getTransform().getPosition().sub(zombie.getTransform().getPosition(), zombie.getAcceleration().getForce());
//        zombie.getAcceleration().getForce().normalize().mul(.015f * zombie.getAcceleration().getFactor());

        camera.setRotation(-45, -45, 0);
        camera.setPosition(
                new Vector3f(player.getTransform().getPosition())
                        .add(0, 2, 0)
                        .sub(camera.getDirection().mul(8))
        );

        world.getSunTransform().setPosition(new Vector3f(50).add(camera.getPosition()));
        world.getCubeTransform().applyRotation(quat -> quat.rotateAxis((float) delta, 1, 1, 1));

        if (floor(pacer.getTime()) != floor(previousTime)) {
            logger.info("Fps: {}, cpu time: {}ms, gpu time: {}ms, gpu delay: {}ms",
                    "%4.1f".formatted(pacer.getAverageFPS()),
                    "%5.2f".formatted(getAvgCpuTime()),
                    "%5.2f".formatted(renderer.getAveragedGpuTime() / 1_000_000.0),
                    "%5.2f".formatted(renderer.getGpuDelay() / 1_000_000.0));
        }
        previousTime = pacer.getTime();
        debugInterface.getFpsLabel().setText(
                "Fps: %d\nRender calls: %d\nTriangles: %d\nMapping: %s\nExposure: %4.2f\nDash cooldown: %.0f".formatted(
                        Math.round(pacer.getAverageFPS()),
                        renderer.getRenderCalls(),
                        renderer.getTrianglesDrawn(),
                        hdrReinhardMapping ? "Reinhard" : "Exposure",
                        hdrExposure,
                        Math.max(0, player.getDashState().getCooldown())
                )
        );
    }

    public static void main(String[] args) {
        new EntityApplication();
    }

}

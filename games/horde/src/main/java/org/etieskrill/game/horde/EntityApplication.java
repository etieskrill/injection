package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.service.*;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCameraController;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;
import java.util.Random;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_SRGB;

public class EntityApplication extends GameApplication {

    private static final int FRAME_RATE = 60;

    private static final Loaders.ModelLoader MODELS = Loaders.ModelLoader.get();

    private static final Logger logger = LoggerFactory.getLogger(EntityApplication.class);

    private Camera camera;

    private DirectionalLight sunLight;
    private Transform sunTransform;
    private boolean light = true;
    private static final Vector3fc lightOn = new Vector3f(1);
    private static final Vector3fc lightOff = new Vector3f(0);

    private Transform cubeTransform;

    private Transform playerTransform;
    private DirectionalForceComponent playerMoveForce; //TODO

    private Label fpsLabel;

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

        Model floorModel = ModelFactory.box(new Vector3f(100, .1f, 100));
        Material floorMaterial = floorModel.getNodes().get(2).getMeshes().getFirst().getMaterial();
        floorMaterial.setProperty(Material.Property.SHININESS, 256f);
        floorMaterial.getTextures().clear();
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_COL_2K_METALNESS.png", DIFFUSE));
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_ROUGHNESS_2K_METALNESS.png", SPECULAR));
        floorMaterial.getTextures().add(
                new Texture2D.FileBuilder("TilesSlateSquare001_NRM_2K_METALNESS.png", NORMAL)
                        .setFormat(AbstractTexture.Format.RGB) //TODO MMMMMMHHHHH select correct format automatically
                        .build()
        );

        Entity floor = entitySystem.createEntity();
        Drawable floorDrawable = new Drawable(floorModel);
        floorDrawable.setTextureScale(new Vector2f(15));
        floor.addComponent(floorDrawable);

        floorModel.getTransform().setPosition(new Vector3f(0, -1, 0));
        floor.addComponent(floorModel.getTransform());

        floor.addComponent(new AABB(
                floorModel.getBoundingBox().getMin().mul(floorModel.getInitialTransform().getScale(), new Vector3f()),
                floorModel.getBoundingBox().getMax().mul(floorModel.getInitialTransform().getScale(), new Vector3f())
        ));
        floor.addComponent(new WorldSpaceAABB());

        floor.addComponent(new StaticCollider());

        Model sphere = MODELS.load("sphere", () -> Model.ofFile("Sphere.obj"));

        Entity sun = entitySystem.createEntity();
        sunLight = new DirectionalLight(new Vector3f(-1), new Vector3f(.1f), new Vector3f(.5f), new Vector3f(2));
        sun.addComponent(sunLight);
        Model sunModel = new Model(sphere);
        sunModel.getTransform().setPosition(new Vector3f(50)).setScale(new Vector3f(.35f));
        sun.addComponent(new Drawable(sunModel));
        sunTransform = new Transform(sunModel.getTransform());
        sun.addComponent(sunTransform);

        PointLight light1 = new PointLight(new Vector3f(10, 0, 10),
                new Vector3f(2f), new Vector3f(5), new Vector3f(5),
                1, .14f, .07f);
        Model lightModel1 = new Model(sphere);
        lightModel1.getTransform().setPosition(light1.getPosition()).setScale(.01f);

        PointLight light2 = new PointLight(new Vector3f(-10, 0, -10),
                new Vector3f(2f), new Vector3f(5), new Vector3f(5),
                1, .14f, .07f);
        Model lightModel2 = new Model(sphere);
        lightModel2.getTransform().setPosition(light2.getPosition()).setScale(.01f);

        MODELS.load("brick-cube", () -> Model.ofFile("brick-cube.obj"));
        Random random = new Random(69420);
        for (int i = 0; i < 10; i++) {
            Model cubeModel = MODELS.get("brick-cube");
            cubeModel.getTransform()
                    .setPosition(new Vector3f(random.nextFloat() * 30 - 15, random.nextFloat() * 3, random.nextFloat() * 30 - 15))
                    .applyRotation(quat -> quat.rotationAxis((float) (random.nextFloat() * 2 * Math.PI - Math.PI),
                            new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
                                    .mul(2).sub(1, 1, 1)
                                    .normalize()))
                    .setScale(3);
            Entity cube = entitySystem.createEntity();
            cube.addComponent(new Drawable(cubeModel));
            cube.addComponent(cubeModel.getTransform());
            cube.addComponent(cubeModel.getBoundingBox());
            cube.addComponent(new WorldSpaceAABB());
            cube.addComponent(new StaticCollider());

            if (i == 0) cubeTransform = cubeModel.getTransform();
        }

        camera = new PerspectiveCamera(window.getSize().toVec());
        window.addCursorInputs(new CursorCameraController(camera));
        KeyCameraController cameraController = new KeyCameraController(camera).setSpeed(3);
        window.addKeyInputs(cameraController);
        window.getCursor().disable();

        window.addKeyInputs(Input.of(
                Input.bind(Keys.Q).to(() -> {
                    light = !light;
                    logger.info("Turning sunlight {}", light ? "on" : "off");

                    sunLight.setAmbient(light ? lightOn : lightOff);
                    sunLight.setDiffuse(light ? lightOn : lightOff);
                    sunLight.setSpecular(light ? lightOn : lightOff);
                }),
                Input.bind(Keys.CTRL).to(() -> {
                    if (cameraController.getSpeed() == 3) cameraController.setSpeed(20);
                    else cameraController.setSpeed(3);
                }),
                Input.bind(Keys.E).to(delta -> playerTransform.getPosition().add(0, .025f, 0)),
                Input.bind(Keys.W).on(Trigger.PRESSED).to(delta -> playerTransform.getPosition().add((float) (double) delta, 0, 0))
        ));

        DirectionalShadowMap directionalShadowMap = DirectionalShadowMap.generate(new Vector2i(1024));
        PointShadowMapArray pointShadowMaps = PointShadowMapArray.generate(new Vector2i(1024), 2);

        Matrix4f sunLightCombined = new Matrix4f()
                .ortho(-30, 30, -30, 30, .1f, 40)
                .mul(new Matrix4f().lookAt(new Vector3f(10, 20, 10), new Vector3f(-10, 0, -10), new Vector3f(0, 1, 0)));
        sun.addComponent(new DirectionalLightComponent(sunLight, directionalShadowMap, sunLightCombined));

        final float pointShadowNearPlane = .1f;
        final float pointShadowFarPlane = 40;

        Entity pointLight1 = entitySystem.createEntity();
        pointLight1.addComponent(lightModel1.getTransform());
        pointLight1.addComponent(new Drawable(lightModel1));
        Matrix4fc[] pointLightCombined1 = pointShadowMaps.getCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light1);
        pointLight1.addComponent(new PointLightComponent(light1, pointShadowMaps, 0, pointLightCombined1, pointShadowFarPlane));

        Entity pointLight2 = entitySystem.createEntity();
        pointLight2.addComponent(lightModel2.getTransform());
        pointLight2.addComponent(new Drawable(lightModel2));
        Matrix4fc[] pointLightCombined2 = pointShadowMaps.getCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light2);
        pointLight2.addComponent(new PointLightComponent(light2, pointShadowMaps, 1, pointLightCombined2, pointShadowFarPlane));

        Shaders.DepthShader depthShader = new Shaders.DepthShader();
        Shaders.DepthCubeMapArrayShader depthCubeMapArrayShader = new Shaders.DepthCubeMapArrayShader();

        glEnable(GL_FRAMEBUFFER_SRGB);

        GLUtils.removeDebugLogging();

        OrthographicCamera uiCamera = new OrthographicCamera(window.getSize().toVec());
        fpsLabel = new Label("", Fonts.getDefault(36));
        fpsLabel.setAlignment(Node.Alignment.TOP_LEFT)
                .setMargin(new Vector4f(10));
        window.setScene(new Scene(new Batch(renderer), new Container(fpsLabel), uiCamera));

        entitySystem.addService(new BoundingBoxService());
        entitySystem.addService(new DirectionalShadowMappingService(renderer, depthShader));
        entitySystem.addService(new PointShadowMappingService(renderer, depthCubeMapArrayShader));
        entitySystem.addService(new RenderService(renderer, camera, window.getSize().toVec()));
        entitySystem.addService(new BoundingBoxRenderService(renderer, camera));
        entitySystem.addService(new PhysicsService());

        Entity player = entitySystem.createEntity();
        playerTransform = new Transform();
        player.addComponent(playerTransform);
        player.addComponent(new AABB(new Vector3f(-.5f, 0, -.5f), new Vector3f(.5f, 2, .5f)));
        player.addComponent(new WorldSpaceAABB());
        player.addComponent(new DynamicCollider(new Vector3f()));
        player.addComponent(new DirectionalForceComponent(new Vector3f(0, -5, 0)));
        playerMoveForce = new DirectionalForceComponent(new Vector3f());
    }

    @Override
    protected void loop(final double delta) {
        sunTransform.setPosition(new Vector3f(50).add(camera.getPosition()));
        cubeTransform.applyRotation(quat -> quat.rotateAxis((float) delta, 1, 1, 1));

        if (pacer.getTotalFramesElapsed() % 60 == 0) {
            logger.info("Fps: {}, cpu time: {}ms, gpu time: {}ms, gpu delay: {}ms",
                    "%4.1f".formatted(pacer.getAverageFPS()),
                    "%5.2f".formatted(getAvgCpuTime()),
                    "%5.2f".formatted(renderer.getAveragedGpuTime() / 1_000_000.0),
                    "%5.2f".formatted(renderer.getGpuDelay() / 1_000_000.0));
        }
        fpsLabel.setText(String.valueOf((int) pacer.getAverageFPS()));
    }

    public static void main(String[] args) {
        new EntityApplication();
    }
}

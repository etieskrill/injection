package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCameraController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_SRGB;

public class Application extends GameApplication {

    private static final int FRAME_RATE = 60;

    private static final Loaders.ModelLoader MODELS = Loaders.ModelLoader.get();

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private Model floor;
    private Model[] brickCubes;
    private Shaders.StaticShader shader;
    private Camera camera;

    private DirectionalLight sun;
    private Model sunModel;
    private PointLight light1;
    private Model lightModel1;
    private PointLight light2;
    private Model lightModel2;
    private Shaders.LightSourceShader lightShader;
    private static boolean light = true;
    private static final Vector3fc lightOn = new Vector3f(1);
    private static final Vector3fc lightOff = new Vector3f(0);

    public Application() {
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
        floor = ModelFactory.box(new Vector3f(100, .1f, 100));
        Material floorMaterial = floor.getNodes().get(2).getMeshes().getFirst().getMaterial();
        floorMaterial.setProperty(Material.Property.SHININESS, 256f);
        floorMaterial.getTextures().clear();
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_COL_2K_METALNESS.png", DIFFUSE));
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_ROUGHNESS_2K_METALNESS.png", SPECULAR));
        floorMaterial.getTextures().add(
                new Texture2D.FileBuilder("TilesSlateSquare001_NRM_2K_METALNESS.png", NORMAL)
                        .setFormat(AbstractTexture.Format.RGB) //TODO MMMMMMHHHHH select correct format automatically
                        .build()
        );
        floor.getTransform().setPosition(new Vector3f(0, -1, 0));

        Model sphere = MODELS.load("sphere", () -> Model.ofFile("Sphere.obj"));

        sun = new DirectionalLight(new Vector3f(-1), new Vector3f(0), new Vector3f(1), new Vector3f(2));
        sunModel = new Model(sphere);
        sunModel.getTransform().setPosition(new Vector3f(50)).setScale(new Vector3f(.35f));

        light1 = new PointLight(new Vector3f(10, 0, 10),
                new Vector3f(0), new Vector3f(2), new Vector3f(2),
                1, .14f, .07f);
        lightModel1 = new Model(sphere);
        lightModel1.getTransform().setPosition(light1.getPosition()).setScale(.01f);

        light2 = new PointLight(new Vector3f(-10, 0, -10),
                new Vector3f(0), new Vector3f(2), new Vector3f(2),
                1, .14f, .07f);
        lightModel2 = new Model(sphere);
        lightModel2.getTransform().setPosition(light2.getPosition()).setScale(.01f);

        MODELS.load("brick-cube", () -> Model.ofFile("brick-cube.obj"));
        brickCubes = new Model[10];
        Random random = new Random(69420);
        for (int i = 0; i < brickCubes.length; i++) {
            Model cube = MODELS.get("brick-cube");
            cube.getTransform().setPosition(new Vector3f(random.nextFloat() * 40 - 20, random.nextFloat() * 4 + 1, random.nextFloat() * 40 - 20));
            brickCubes[i] = cube;
        }

        lightShader = Shaders.getLightSourceShader();
        shader = Shaders.getStandardShader();

        camera = new PerspectiveCamera(new Vector2f(window.getSize().toVec()));
        window.addCursorInputs(new CursorCameraController(camera));
        KeyCameraController cameraController = new KeyCameraController(camera).setSpeed(3);
        window.addKeyInputs(cameraController);
        window.getCursor().disable();

        window.addKeyInputs(Input.of(
                Input.bind(Keys.Q).to(() -> {
                    light = !light;
                    logger.info("Turning sunlight {}", light ? "on" : "off");

                    sun.setAmbient(light ? lightOn : lightOff);
                    sun.setDiffuse(light ? lightOn : lightOff);
                    sun.setSpecular(light ? lightOn : lightOff);
                }),
                Input.bind(Keys.CTRL).to(() -> {
                    if (cameraController.getSpeed() == 3) cameraController.setSpeed(20);
                    else cameraController.setSpeed(3);
                })
        ));

        glEnable(GL_FRAMEBUFFER_SRGB);
    }

    @Override
    protected void loop(double delta) {
        shader.setGlobalLights(sun);
        shader.setLights(new PointLight[] {light1, light2});
        shader.setViewPosition(camera.getPosition());
        shader.setTextureScale(new Vector2f(15));
        renderer.render(floor, shader, camera.getCombined());

        shader.setTextureScale(new Vector2f(1));
        for (Model brickCube : brickCubes) renderer.render(brickCube, shader, camera.getCombined());

        lightShader.setLight(sun);
        renderer.render(sunModel, lightShader, camera.getCombined());
        lightShader.setLight(light1);
        renderer.render(lightModel1, lightShader, camera.getCombined());
        lightShader.setLight(light2);
        renderer.render(lightModel2, lightShader, camera.getCombined());
    }

    public static void main(String[] args) {
        new Application();
    }
}

package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCameraController;
import org.etieskrill.engine.window.Window;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application extends GameApplication {

    private static final int FRAME_RATE = 60;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private Model floor;
    private Shaders.StaticShader shader;
    private Camera camera;

    private DirectionalLight sun;
    private Model sunModel;
    private Shaders.LightSourceShader sunShader;
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
//        window.setScene(new Scene(
//                new Batch((GLRenderer) renderer),
//                new Container(
//                        new Label("Horde", Fonts.getDefault(128)).setAlignment(Node.Alignment.CENTER)),
//                new OrthographicCamera(window.getSize().toVec())
//                        .setPosition(new Vector3f(window.getSize().toVec().mul(.5f), 0))
//        ));

        floor = ModelFactory.box(new Vector3f(100, .1f, 100));
        Material floorMaterial = floor.getNodes().get(2).getMeshes().getFirst().getMaterial();
        floorMaterial.setProperty(Material.Property.SHININESS, 64f);
//        floorMaterial.setProperty(Material.Property.SHININESS_STRENGTH, .5f);
        floorMaterial.getTextures().clear();
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_COL_2K_METALNESS.png", AbstractTexture.Type.DIFFUSE));
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_ROUGHNESS_2K_METALNESS.png", AbstractTexture.Type.SPECULAR));
        floor.getTransform().setPosition(new Vector3f(0, -1, 0));

        sun = new DirectionalLight(new Vector3f(-1));
        sunModel = Model.ofFile("Sphere.obj");
        sunModel.getTransform().setPosition(new Vector3f(50)).setScale(new Vector3f(.5f));
        sunShader = Shaders.getLightSourceShader();

        shader = Shaders.getStandardShader();
        shader.setUniform("uTextureScale", new Vector2f(15));

        camera = new PerspectiveCamera(new Vector2f(window.getSize().toVec()));
        window.addCursorInputs(new CursorCameraController(camera));
        KeyCameraController cameraController = new KeyCameraController(camera).setSpeed(3);
        window.addKeyInputs(cameraController);
        window.getCursor().disable();

        window.addKeyInputs(Input.of(
                Input.bind(Keys.Q).to(() -> {
                    light = !light;
                    logger.info("Turning light {}", light ? "on" : "off");

                    sun.setAmbient(light ? lightOn : lightOff);
                    sun.setDiffuse(light ? lightOn : lightOff);
//                    sun.setSpecular(light ? lightOn : lightOff);
                }),
                Input.bind(Keys.CTRL).to(() -> {
                    if (cameraController.getSpeed() == 3) cameraController.setSpeed(20);
                    else cameraController.setSpeed(3);
                })
        ));
    }

    @Override
    protected void loop(double delta) {
        shader.setGlobalLights(sun);
        shader.setViewPosition(camera.getPosition());
        renderer.render(floor, shader, camera.getCombined());

        sunShader.setLight(sun);
        renderer.render(sunModel, sunShader, camera.getCombined());
    }

    public static void main(String[] args) {
        new Application();
    }
}

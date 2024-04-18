package org.etieskrill.game.shaderz;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.scene.component.VBox;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.window.Window;
import org.joml.*;

import java.lang.Math;
import java.util.Arrays;

public class Game {

    private Window window = new Window.Builder()
            .setTitle("Some Shader Buggery")
            .setMode(Window.WindowMode.BORDERLESS)
            .setVSyncEnabled(true)
            .setSamples(4)
            .build();

    private Camera camera = new PerspectiveCamera(window.getSize().toVec());

    private Model hallway;
    private Model sun;
    private DirectionalLight sunLight;

    private Renderer renderer = new GLRenderer();
    private final ShaderProgram shader = new HallwayShader();
    private final ShaderProgram sunShader = Shaders.getLightSourceShader();

    private LoopPacer pacer = new SystemNanoTimePacer(1 / 60f);

    private Label fpsLabel, verticesDrawnLabel, primitivesDrawnLabel; //TODO use these for performance evaluation

    public Game() {
        window.addKeyInputs(Input.of(
                Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(() -> window.close())
        ));
        window.getCursor().disable();

        window.addCursorInputs(
                new CursorCameraController(camera, .05, 0)
        );

        hallway = new Model.Builder("scifi-hallway.glb").disableCulling().build(); //TODO implement per-mesh culling, then enable here

        sun = Model.ofFile("box.obj");
        sun.getTransform().setScale(0).setPosition(new Vector3f(0, 10, 0));
        sunLight = new DirectionalLight(sun.getTransform().getPosition().normalize(), new Vector3f(.2f), new Vector3f(.5f), new Vector3f(.5f));

        camera.setFar(500);
        camera.orient(0, 0, 0);
        camera.setPosition(new Vector3f(0, 4, 0));

        fpsLabel = (Label) new Label("", Fonts.getDefault(48)).setMargin(new Vector4f(10));
        window.setScene(new Scene(
                new Batch((GLRenderer) renderer).setShader(Shaders.getTextShader()),
                new VBox(fpsLabel).setAlignment(Node.Alignment.TOP_LEFT),
                new OrthographicCamera(window.getSize().toVec()))
        );

        loop();
    }

    private static final int NUM_SECTORS = 80;
    private static final float ANGLE = 5;

    private void loop() {
        pacer.start();
        while (!window.shouldClose()) {
            sun.getTransform().setPosition(new Vector3f((float) (10 * Math.cos(pacer.getTime())), 10, (float) (10 * Math.sin(pacer.getTime()))));
            sunLight.setDirection(sun.getTransform().getPosition().negate().normalize());

            Matrix4fc[] hallwaySegments = new Matrix4fc[NUM_SECTORS];
            hallwaySegments[0] = hallway.getTransform().getMatrix();
            for (int i = 1; i < hallwaySegments.length; i++) {
                Vector3f translation = hallwaySegments[i - 1]
                        .transformPosition(new Vector3f(hallway.getBoundingBox().getSize().x(), 0, 0));
                int finalI = i;
                hallwaySegments[i] = new Transform(hallway.getTransform())
                        .translate(translation)
                        .applyRotation(rotation -> rotation.rotationZ((float) (finalI * Math.toRadians(ANGLE))))
                        .getMatrix();
            }
            shader.setUniformArray("uModels[$]", hallwaySegments);
            shader.setUniformArray("uNormals[$]", Arrays.stream(hallwaySegments).map(mat4 -> mat4.invert(new Matrix4f()).transpose().get3x3(new Matrix3f())).toArray());

            fpsLabel.setText(String.valueOf((int) pacer.getAverageFPS()));

            render();

            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }
    }

    private void render() {
        renderer.prepare();

        shader.setUniform("uTime", (float) pacer.getTime(), false);
        shader.setUniform("sun", sunLight);
        sunShader.setUniform("light", sunLight);

        renderer.render(sun, sunShader, camera.getCombined());
        renderer.renderInstances(hallway, NUM_SECTORS, shader, camera.getCombined());
    }

    public static void main(String[] args) {
        new Game();
    }

}

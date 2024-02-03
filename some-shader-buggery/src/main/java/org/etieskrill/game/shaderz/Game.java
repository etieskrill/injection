package org.etieskrill.game.shaderz;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.OrthographicCamera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.DirectionalLight;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node;
import org.etieskrill.engine.scene.component.VBox;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.window.Window;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class Game {

    private Window window = new Window.Builder()
            .setTitle("Some Shader Buggery")
            .setMode(Window.WindowMode.BORDERLESS)
            .setVSyncEnabled(true)
            .setSamples(4)
            .build();

    private Camera camera = new PerspectiveCamera(window.getSize().toVec());
    private final Vector2f prevCursorPos;

    private Model hallway;
    private Model sun;
    private DirectionalLight sunLight;

    private Renderer renderer = new Renderer();
    private final ShaderProgram shader = new HallwayShader();
    private final ShaderProgram sunShader = Shaders.getLightSourceShader();

    private LoopPacer pacer = new SystemNanoTimePacer(1 / 60f);

    private Label fpsLabel, verticesDrawnLabel, primitivesDrawnLabel;

    public Game() {
        window.setInputs(Input.of(
                Input.bind(Keys.ESC.withMods(Keys.Mod.SHIFT)).to(() -> window.close())
        ));
        window.getCursor().disable();

        final double sensitivity = 0.05;
        prevCursorPos = window.getCursor().getPosition();
        GLFW.glfwSetCursorPosCallback(window.getID(), (window, xpos, ypos) -> {
            camera.orient(
                    -sensitivity * (prevCursorPos.getY() - ypos),
                    sensitivity * (prevCursorPos.getX() - xpos), 0);
            prevCursorPos.set(xpos, ypos);
        });

        hallway = new Model.Builder("scifi-hallway.glb").disableCulling().build(); //TODO implement per-mesh culling, then enable here

        sun = Model.ofFile("box.obj");
        sun.getTransform().setScale(0).setPosition(new Vector3f(0, 10, 0));
        sunLight = new DirectionalLight(sun.getTransform().getPosition().normalize(), new Vector3f(0.2), new Vector3f(0.5), new Vector3f(0.5));

        camera.setFar(500);
        camera.orient(0, 0, 0);
        camera.setPosition(new Vector3f(0, 4, 0));

        fpsLabel = (Label) new Label("", Fonts.getDefault(48)).setMargin(new Vector4f(10));
        window.setScene(new Scene(
                new Batch(renderer).setShader(Shaders.getTextShader()),
                new VBox(fpsLabel).setAlignment(Node.Alignment.TOP_LEFT),
                new OrthographicCamera(window.getSize().toVec()).setPosition(new Vector3f(window.getSize().toVec().mul(0.5), 0)))
        );

        loop();
    }

    private static final int NUM_SECTORS = 80;
    private static final float ANGLE = 5;

    private void loop() {
        pacer.start();
        while (!window.shouldClose()) {
            sun.getTransform().setPosition(new Vector3f(10 * Math.cos(pacer.getTime()), 10, 10 * Math.sin(pacer.getTime())));
            sunLight.setDirection(sun.getTransform().getPosition().negate().normalize());

            Matrix4f[] hallwaySegments = new Matrix4f[NUM_SECTORS];
            hallwaySegments[0] = hallway.getTransform().toMat();
            for (int i = 1; i < hallwaySegments.length; i++) {
                Vector3f translation = hallwaySegments[i - 1]
                        .mul(new Vector4f((float) hallway.getBoundingBox().getSize().getX(), 0, 0, 1))
                        .toVector3f();
                hallwaySegments[i] = new Transform(hallway.getTransform())
                        .translate(translation)
                        .setRotation((float) (i * Math.toRadians(ANGLE)), new Vector3f(0, 0, 1))
                        .toMat();
            }
            shader.setUniformArray("uModels[$]", hallwaySegments);
            shader.setUniformArray("uNormals[$]", Arrays.stream(hallwaySegments).map(mat4 -> mat4.inverseTranspose().toMat3()).toArray());

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

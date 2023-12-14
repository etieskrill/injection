package org.etieskrill.game.shaderz;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.DirectionalLight;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.window.Window;
import org.lwjgl.glfw.GLFW;

public class Game {

    private Window window = new Window.Builder()
            .setTitle("Some Shader Buggery")
            .setMode(Window.WindowMode.BORDERLESS)
            .build();

    private Camera camera = new PerspectiveCamera(window.getSize().toVec());
    private final Vec2 prevCursorPos;

    private Model hallway;
    private Model sun;
    private DirectionalLight sunLight;

    private Renderer renderer = new Renderer();
    private final ShaderProgram shader = new HallwayShader();
    private final ShaderProgram sunShader = Shaders.getLightSourceShader();

    private LoopPacer pacer = new SystemNanoTimePacer(1 / 60f);

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
            prevCursorPos.put(xpos, ypos);
        });

        hallway = new Model.Builder("scifi-hallway.glb").build();
        sun = Model.ofFile("box.obj");
        sun.getTransform().setScale(0).setPosition(new Vec3(0, 10, 0));
        sunLight = new DirectionalLight(sun.getTransform().getPosition().normalize(), new Vec3(1), new Vec3(1), new Vec3(1));

        camera.orient(0, 0, 0);
        camera.setPosition(new Vec3(-6, 3, 0));

        loop();
    }

    private void loop() {
        pacer.start();
        while (!window.shouldClose()) {
            sun.getTransform().setPosition(new Vec3(10 * Math.cos(pacer.getTime()), 10, 10 * Math.sin(pacer.getTime())));
            sunLight.setDirection(sun.getTransform().getPosition().negate().normalize());

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
        renderer.render(hallway, shader, camera.getCombined());
    }

    public static void main(String[] args) {
        new Game();
    }

}

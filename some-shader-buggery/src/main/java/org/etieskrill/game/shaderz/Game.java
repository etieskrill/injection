package org.etieskrill.game.shaderz;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
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

    private Renderer renderer = new Renderer();
    private final ShaderProgram shader = new HallwayShader();

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

        this.hallway = new Model.Builder("box.obj").disableCulling().build();
        hallway.getTransform().setInitialScale(new Vec3(5, 5, 20)).setInitialPosition(new Vec3(-6, -5, -5));

        loop();
    }

    private void loop() {
        pacer.start();
        while (!window.shouldClose()) {
//            hallway.getTransform().setRotation((float) (hallway.getTransform().getRotation() + pacer.getDeltaTimeSeconds()), new Vec3(0, 1, 0));

            render();

            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }
    }

    private void render() {
        renderer.prepare();

        shader.setUniform("uTime", (float) pacer.getTime());

//        renderer.render(hallway, shader, camera.getCombined());
        renderer.renderWireframe(hallway, shader, camera.getCombined());
    }

    public static void main(String[] args) {
        new Game();
    }

}

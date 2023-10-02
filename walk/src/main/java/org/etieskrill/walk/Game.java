package org.etieskrill.walk;

import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.input.*;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_BACK;

public class Game {
    
    private Window window;
    
    private Camera camera;
    
    private double prevCursorPosX, prevCursorPosY;
    
    private InputManager inputManager = InputBinds.of(
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_ESCAPE, GLFW_MOD_SHIFT), () -> window.close()),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_W), Trigger.PRESSED, delta -> camera.translate(new Vec3(0, 0, 1).times(delta))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_S), Trigger.PRESSED, delta -> camera.translate(new Vec3(0, 0, -1).times(delta))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_A), Trigger.PRESSED, delta -> camera.translate(new Vec3(-1, 0, 0).times(delta))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_D), Trigger.PRESSED, delta -> camera.translate(new Vec3(1, 0, 0).times(delta))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_SPACE), Trigger.PRESSED, delta -> camera.translate(new Vec3(0, -1, 0).times(delta))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_LEFT_SHIFT), Trigger.PRESSED, delta -> camera.translate(new Vec3(0, 1, 0).times(delta))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_E), Trigger.ON_PRESS, () -> System.out.println("*poof*"))
    );
    
    public Game() {
        setupWindow();
        loop();
        exit();
    }
    
    private void setupWindow() {
        //TODO 1. figure out what this "context" actually is 2. make window creation disjoint from gl context creation
        window = new Window.Builder()
                .setMode(Window.WindowMode.FULLSCREEN)
                .setTitle("Walk")
                .setInputManager(inputManager)
                .build();
        
        setupCursor();
        window.getCursor().disable();
        
        GL.createCapabilities();
    
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_BLEND);
    
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }
    
    private void setupCursor() {
        glfwSetCursorPosCallback(window.getID(), ((window1, xpos, ypos) -> {
            double dx = prevCursorPosX - xpos;
            double dy = prevCursorPosY - ypos;

            double sens = 0.04;
            camera.orient(dy * sens, -dx * sens, 0);
    
            prevCursorPosX = xpos;
            prevCursorPosY = ypos;
        }));
    }
    
    private void loop() {
        LoopPacer pacer = new SystemNanoTimePacer(1 / 60f);
        
        Model cube = Loaders.ModelLoader.get().load("cube", () -> Model.ofFile("cube.obj"));
        Renderer renderer = new Renderer();
        ShaderProgram shader = Loaders.ShaderLoader.get().load("standard", () -> Shaders.getStandardShader());
        
        camera = new PerspectiveCamera(window.getSize().toVec())
                .setPosition(new Vec3(0, 0, -5))
                .setOrientation(0, 90, 0);
        
        pacer.start();
        while (!window.shouldClose()) {
            renderer.prepare();
            renderer.render(cube, shader, camera.getCombined());
            
            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }
    }
    
    private void exit() {
        window.dispose();
        Loaders.disposeDefaultLoaders();
        System.exit(0);
    }
    
    public static void main(String[] args) {
        new Game();
    }
    
}

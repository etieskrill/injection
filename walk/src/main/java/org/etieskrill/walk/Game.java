package org.etieskrill.walk;

import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.PointLight;
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
    
    private final Vec3 deltaPos = new Vec3(0);
    private float rotation, smoothRotation;
    private boolean jump = false;
    private float jumpTime;
    private boolean crouch = false;
    
    private double prevCursorPosX, prevCursorPosY;
    
    private InputManager inputManager = InputBinds.of(
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_ESCAPE, GLFW_MOD_SHIFT), () -> window.close()),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_W), Trigger.PRESSED, delta -> deltaPos.plusAssign(new Vec3(0, 0, 1))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_S), Trigger.PRESSED, delta -> deltaPos.plusAssign(new Vec3(0, 0, -1))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_A), Trigger.PRESSED, delta -> deltaPos.plusAssign(new Vec3(-1, 0, 0))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_D), Trigger.PRESSED, delta -> deltaPos.plusAssign(new Vec3(1, 0, 0))),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_SPACE), Trigger.PRESSED, () -> jump = true),
            new InputBinding(new KeyInput(KeyInput.Type.KEY, GLFW_KEY_LEFT_SHIFT), Trigger.PRESSED, () -> crouch = true),
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
            camera.orient(-dy * sens, dx * sens, 0);
    
            prevCursorPosX = xpos;
            prevCursorPosY = ypos;
        }));
    }
    
    private void loop() {
        //TODO figure out a smart way to link the pacer and window refresh rates
        LoopPacer pacer = new SystemNanoTimePacer(1 / 60f);
        
        Model cube = Loaders.ModelLoader.get().load("cube", () -> Model.ofFile("cube.obj")).setScale(50).setPosition(new Vec3(0, -25, 0));
        Model light = Loaders.ModelLoader.get().get("cube").setScale(0.5f).setPosition(new Vec3(2, 5, -2));
        Model skelly = Loaders.ModelLoader.get().load("skelly", () -> new Model.Builder("skeleton.glb").build().setScale(15));
        Renderer renderer = new Renderer();
        ShaderProgram shader = Loaders.ShaderLoader.get().load("standard", () -> Shaders.getStandardShader());
        ShaderProgram lightShader = Loaders.ShaderLoader.get().load("light", () -> Shaders.getLightSourceShader());
        
        camera = new PerspectiveCamera(window.getSize().toVec());
    
        PointLight pointLight = new PointLight(light.getPosition(), new Vec3(1), new Vec3(1), new Vec3(1),
                1f, 0.03f, 0.005f);
        
        pacer.start();
        while (!window.shouldClose()) {
            skelly
                    .translate(
                            camera.relativeTranslation(deltaPos.length() != 0 ?
                                    deltaPos.normalize().times((float) pacer.getDeltaTimeSeconds() * 4) :
                                    new Vec3(0)).times(1, 0, 1));
            if (deltaPos.anyNotEqual(0, 0.001f)) {
                rotation = (float) (Math.atan2(deltaPos.getZ(), deltaPos.getX()) - Math.toRadians(camera.getYaw()));
                rotation %= Math.toRadians(360);
                //TODO fix shortest distance through wraparound
                if (Math.abs(rotation - smoothRotation) > 0.001) smoothRotation += Math.toRadians(rotation - smoothRotation >= 0 ? 3 : -3);
                smoothRotation %= Math.toRadians(360);
                skelly.setRotation(
                        smoothRotation,
                        new Vec3(0, 1, 0));
            }
            deltaPos.put(0);
            
            if (jump && jumpTime == 0)
                jumpTime += 0.0001;
            if (jumpTime != 0 && jumpTime < 1) {
                double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
                skelly.getPosition().setY((float) jumpHeight);
                jumpTime += pacer.getDeltaTimeSeconds();
            } else jumpTime = 0;
            jump = false;
            
            if (crouch) skelly.setScale(new Vec3(15, 9, 15));
            else skelly.setScale(15);
            crouch = false;
            
            Vec3 orbitPos = camera.getDirection().negate().times(3);
            camera.setPosition(orbitPos.plus(0, 2.5, 0).plus(skelly.getPosition()));
            
            renderer.prepare();
            shader.setUniform("uViewPosition", camera.getPosition());
            shader.setUniformArray("lights[$]", 0, pointLight);
            renderer.render(cube, shader, camera.getCombined());
            renderer.render(skelly, shader, camera.getCombined());
            
            lightShader.setUniform("light", pointLight);
            renderer.render(light, lightShader, camera.getCombined());
            
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

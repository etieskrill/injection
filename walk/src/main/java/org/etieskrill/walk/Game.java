package org.etieskrill.walk;

import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.PointLight;
import org.etieskrill.engine.input.InputBinding;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.InputBinds;
import org.etieskrill.engine.input.InputManager;
import org.etieskrill.engine.input.KeyInput.Keys;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;

public class Game {
    
    private Window window;
    
    private Camera camera;
    
    private final Vec3 deltaPos = new Vec3(0);
    private float rotation, smoothRotation;
    private float jumpTime;
    
    private double prevCursorPosX, prevCursorPosY;
    
    private final InputManager inputManager = InputBinds.of(
            new InputBinding(Keys.ESC.withMods(Keys.SHIFT), () -> window.close()),
            new InputBinding(Keys.W, Trigger.PRESSED, () -> deltaPos.plusAssign(new Vec3(0, 0, deltaPos.getZ() < 0 ? 2 : 1))),
            new InputBinding(Keys.S, Trigger.PRESSED, () -> deltaPos.plusAssign(new Vec3(0, 0, deltaPos.getZ() > 0 ? -2 : -1))),
            new InputBinding(Keys.A, Trigger.PRESSED, () -> deltaPos.plusAssign(new Vec3(-1, 0, 0))),
            new InputBinding(Keys.D, Trigger.PRESSED, () -> deltaPos.plusAssign(new Vec3(1, 0, 0))),
            new InputBinding(Keys.Q, Trigger.ON_TOGGLE, () -> System.out.println("*bang*")),
            new InputBinding(Keys.E, Trigger.ON_PRESS, () -> System.out.println("*poof*"))
    );
    
    public Game() {
        setupWindow();
        loop();
        exit();
    }
    
    private void setupWindow() {
        window = new Window.Builder()
                .setMode(Window.WindowMode.BORDERLESS)
                .setTitle("Walk")
                .setInputManager(inputManager)
                .build();
        
        setupCursor();
        window.getCursor().disable();
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
        Shaders.StaticShader shader = (Shaders.StaticShader) Loaders.ShaderLoader.get().load("standard", Shaders::getStandardShader);
        Shaders.LightSourceShader lightShader = (Shaders.LightSourceShader) Loaders.ShaderLoader.get().load("light", Shaders::getLightSourceShader);
        
        camera = new PerspectiveCamera(window.getSize().toVec());

        PointLight pointLight = new PointLight(light.getPosition(), new Vec3(1), new Vec3(1), new Vec3(1),
                1f, 0.03f, 0.005f);
        PointLight[] pointLights = {pointLight};
        
        pacer.start();
        while (!window.shouldClose()) {
            Vec3 skellyTranslate = camera
                    .relativeTranslation(deltaPos)
                    .times(1, 0, 1);
            if (skellyTranslate.length2() > 0) skellyTranslate.normalizeAssign();
            skellyTranslate = skellyTranslate
                    .times(pacer.getDeltaTimeSeconds())
                    .times(5);
            skelly.translate(skellyTranslate);
            
            if (deltaPos.anyNotEqual(0, 0.001f)) {
                rotation = (float) (Math.atan2(deltaPos.getZ(), deltaPos.getX()) - Math.toRadians(camera.getYaw()));
                rotation %= Math.toRadians(360);
                //TODO fix shortest distance through wraparound
                if (Math.abs(rotation - smoothRotation) > 0.001) {
                    double falloff = -0.5 * (1 / (2 * Math.abs(rotation - smoothRotation) + 0.5)) + 1;
                    smoothRotation += Math.toRadians(rotation - smoothRotation >= 0 ? 5 * falloff : -5 * falloff);
                }
                smoothRotation %= Math.toRadians(360);
                skelly.setRotation(
                        smoothRotation,
                        new Vec3(0, 1, 0));
            }
            deltaPos.put(0);
            
            if (inputManager.isPressed(Keys.SPACE) && jumpTime == 0)
                jumpTime += 0.0001;
            if (jumpTime != 0 && jumpTime < 1) {
                double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
                skelly.getPosition().setY((float) jumpHeight);
                jumpTime += pacer.getDeltaTimeSeconds();
            } else jumpTime = 0;
            
            if (inputManager.isPressed(Keys.LEFT_SHIFT)) skelly.setScale(new Vec3(15, 9, 15));
            else skelly.setScale(15);
            
            Vec3 orbitPos = camera.getDirection().negate().times(3);
            camera.setPosition(orbitPos.plus(0, 2.5, 0).plus(skelly.getPosition()));

            //TODO add collidable border wall
            //     add collectible orbs
            //     add pause menu
            //     add start menu
            //     add victory screen / with score
            //     add animation, fixed / skeletal
            //     create component system
            //     pack common components
            //     create "world" for updateable objects
            //     abstract all gl/ram resources from objects and share accordingly

            //TODO meta
            //     editorconfig
            
            renderer.prepare();
            shader.setViewPosition(camera.getPosition());
            shader.setLights(pointLights);
            
            renderer.render(cube, shader, camera.getCombined());
            renderer.render(skelly, shader, camera.getCombined());
            
            lightShader.setLight(pointLight);
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

package org.etieskrill.game;

import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.graphics.gl.*;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWWindow {
    
    private static final double TARGET_FPS = 60d;
    
    private long
            primaryMonitor,
            window;
    
    public GLFWWindow() {
        init();
        loop();
        
        //glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        
        glfwTerminate();
    }
    
    private void init() {
        glfwSetErrorCallback(GLFWErrorCallback.createPrint());
    
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize glfw");
    
        glfwSetErrorCallback((retVal, argv) -> {
            PointerBuffer errorMessage = BufferUtils.createPointerBuffer(1);
            throw new IllegalStateException(String.format("GLFW error ocurred: + %d\nMessage: %s",
                    glfwGetError(errorMessage), errorMessage.getStringASCII()));
        });
        
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        }
        
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    
        primaryMonitor = glfwGetPrimaryMonitor();
        if (primaryMonitor == MemoryUtil.NULL)
            throw new IllegalStateException("Could not find primary monitor");
        
        GLFWVidMode primaryVidMode = glfwGetVideoMode(primaryMonitor);
        window = glfwCreateWindow(primaryVidMode.width(), primaryVidMode.height(),
                "Test", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new IllegalStateException("Could not create glfw window");
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); //TODO why does it break when 4<?
        
        GL.createCapabilities();
        GLUtil.setupDebugMessageCallback(System.out); //TODO unbind when done
        
        if (!initKeybinds()) throw new IllegalStateException("Could not initialise keybinds");
        
        glfwShowWindow(window);
    }
    
    private boolean initKeybinds() {
        glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
            if (key == GLFW_KEY_ESCAPE && (mods & GLFW_MOD_SHIFT) != 0) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        return true;
    }
    
    private void loop() {
        int displayCounter = 0;
        double accumulatedFPS = 0;
        double time = 0;
        LoopPacer pacer = new SystemNanoTimePacer(1d / TARGET_FPS);
        
        Loader loader = new Loader();

        ModelFactory factory = new ModelFactory();

        MovableModel model1 = new MovableModel(factory.rectangle(-0.25f, -0.25f, 0.5f, 0.5f));
        RawModel model2 = factory.circleSect(-0.5f, 0.5f, 0.15f, 0, 150, 8);
        RawModel model3 = factory.circle(0, 0, 0.2f, 20);
    
        Renderer renderer = new Renderer();
        StaticShader shader = new StaticShader();
        
        pacer.start();
        
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            
            Vec2f deltaPosition = new Vec2f(0.1f, 0.1f).scl((float) pacer.getSecondsSinceLastFrame());
            model1.updatePosition(model1.getPosition().add(deltaPosition));
            
            renderer.prepare();
            shader.start();
            renderer.render(model1);
            //renderer.render(model2);
            //renderer.render(model3);
            shader.stop();
            
            if (displayCounter++ > 60) {
                //System.out.printf("%.3f\n", 1 / (now - time));
                displayCounter = 0;
                //System.out.printf("%.3f\n", accumulatedFPS / 60f);
                //accumulatedFPS = 0;
                System.out.printf("%.3f\n", pacer.getAverageFPS());
            } else {
                //accumulatedFPS += 1 / (now - time);
            }
        
            time = now;
        
            glfwSwapBuffers(window);
            glfwPollEvents(); //Also proves to system that window has not frozen
            
            pacer.nextFrame();
        }
        
        shader.dispose();
        loader.cleanup();
    }
    
    public static void main(String[] args) {
        new GLFWWindow();
    }
    
}

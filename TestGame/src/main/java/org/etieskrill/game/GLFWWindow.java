package org.etieskrill.game;

import org.etieskrill.engine.graphics.gl.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

public class GLFWWindow {
    
    private long
            primaryMonitor,
            window;
    
    public GLFWWindow() {
        init();
        loop();
    
        //glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        
        GLFW.glfwTerminate();
    }
    
    private void init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint());
    
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize glfw");
    
        GLFW.glfwSetErrorCallback((retVal, argv) -> {
            PointerBuffer errorMessage = BufferUtils.createPointerBuffer(1);
            throw new IllegalStateException(String.format("GLFW error ocurred: + %d\nMessage: %s",
                    GLFW.glfwGetError(errorMessage), errorMessage.getStringASCII()));
        });
        
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        if (Platform.get() == Platform.MACOSX) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        }
        
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    
        primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        if (primaryMonitor == MemoryUtil.NULL)
            throw new IllegalStateException("Could not find primary monitor");
        
        GLFWVidMode primaryVidMode = GLFW.glfwGetVideoMode(primaryMonitor);
        window = GLFW.glfwCreateWindow(primaryVidMode.width(), primaryVidMode.height(),
                "Test", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new IllegalStateException("Could not create glfw window");
        
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); //TODO why does it break when 4<?
        
        GL.createCapabilities();
        GLUtil.setupDebugMessageCallback(System.out); //TODO unbind when done
        
        GLFW.glfwShowWindow(window);
    }
    
    private void loop() {
        int displayCounter = 0;
        double accumulatedFPS = 0;
        double time = 0;
        
        Loader loader = new Loader();

        ShapeModelFactory factory = new ShapeModelFactory();

        RawMemoryModel model1 = factory.rectangle(-0.5f, -0.5f, 1f, 1f);
        RawMemoryModel model2 = factory.circleSect(-0.5f, 0.5f, 0.15f, 0, 150, 8);
        RawMemoryModel model3 = factory.circle(0, 0, 0.2f, 20);
    
        Renderer renderer = new Renderer();
        StaticShader shader = new StaticShader();
        
        while (!GLFW.glfwWindowShouldClose(window)) {
            double now = GLFW.glfwGetTime();

            renderer.prepare();
            shader.start();
            renderer.render(model1);
            renderer.render(model2);
            renderer.render(model3); 
            shader.stop();
            
            if (displayCounter++ > 60) {
                //System.out.printf("%.3f\n", 1 / (now - time));
                displayCounter = 0;
                System.out.printf("%.3f\n", accumulatedFPS / 60f);
                accumulatedFPS = 0;
            } else {
                accumulatedFPS += 1 / (now - time);
            }
        
            time = now;
        
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents(); //Also proves to system that window has not frozen
        }
        
        shader.dispose();
        loader.cleanup();
    }
    
    public static void main(String[] args) {
        new GLFWWindow();
    }
    
}

package org.etieskrill.game;

import org.etieskrill.engine.graphics.gl.shaders.ShaderFactory;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.graphics.gl.*;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GLFWWindow {
    
    private static final double TARGET_FPS = 60d;

    private volatile boolean wPressed, aPressed, sPressed, dPressed;
    private MovableModel model1;
    private LoopPacer pacer;

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
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        if (Platform.get() == Platform.MACOSX)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
    
        primaryMonitor = glfwGetPrimaryMonitor();
        if (primaryMonitor == NULL)
            throw new IllegalStateException("Could not find primary monitor");
        
        GLFWVidMode primaryVidMode = glfwGetVideoMode(primaryMonitor);
        window = glfwCreateWindow(primaryVidMode.width(), primaryVidMode.height(),
                "Test", NULL, NULL);
        if (window == NULL)
            throw new IllegalStateException("Could not create glfw window");
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); //TODO why does it break when 4<?
        
        GL.createCapabilities();
        GLUtil.setupDebugMessageCallback(System.out); //TODO unbind when done
        //GL33C.glViewport(0, 0, 1920, 1080); //this is apparently done ... somewhere behind the scenes?
        
        if (!initKeybinds()) throw new IllegalStateException("Could not initialise keybinds");
        
        glfwShowWindow(window);
    }
    
    private boolean initKeybinds() {
        //TODO via callback or via glfwGetKey? either option function by polling, so difference is only in saved state
        // concurrency?
        // acktshually the callback should save some polling cycles, since input events are not read every render cycle,
        // unlike with the getKey method, but this should and will be addressed as soon as it becomes an issue, whether
        // of performance or otherwise, and no sooner. timed tests will decide final result
        
        glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
            if (key == GLFW_KEY_ESCAPE && (mods & GLFW_MOD_SHIFT) != 0) {
                glfwSetWindowShouldClose(window, true);
            } else if (key == GLFW_KEY_W) {
                wPressed = action != GLFW_RELEASE;
            } else if (key == GLFW_KEY_A) {
                aPressed = action != GLFW_RELEASE;
            } else if (key == GLFW_KEY_S) {
                sPressed = action != GLFW_RELEASE;
            } else if (key == GLFW_KEY_D) {
                dPressed = action != GLFW_RELEASE;
            }
        });
        
        return true;
    }

    private void loop() {
        pacer = new SystemNanoTimePacer(1d / TARGET_FPS, 5);

        Loader loader = new Loader();

        ModelFactory factory = new ModelFactory();

        model1 = new MovableModel(factory.rectangle(-0.25f, -0.25f, 0.5f, 0.5f));
        RawModel model2 = factory.circleSect(-0.5f, 0.5f, 0.15f, 0, 150, 8);
        RawModel model3 = factory.circle(0, 0, 0.2f, 20);
        MovableModel model4 = new MovableModel(factory.rectangle(-1f, -1f, 2f, 2f));

        MovableModelList model5 = factory.roundedRect(-0.25f, -0.25f, 0.5f, 0.5f, 0.03f, 8);
    
        Renderer renderer = new Renderer();
        ShaderProgram shader = ShaderFactory.getStandardShader();
        
        pacer.start();
        
        while (!glfwWindowShouldClose(window)) {
            Vec2f newPosition = new Vec2f();
            if (wPressed) newPosition.set(newPosition.add(new Vec2f(0f, 1f)));
            if (dPressed) newPosition.set(newPosition.add(new Vec2f(1f, 0f)));
            if (sPressed) newPosition.set(newPosition.add(new Vec2f(0f, -1f)));
            if (aPressed) newPosition.set(newPosition.add(new Vec2f(-1f, 0f)));

            Vec2f deltaPosition = newPosition.scl((float) pacer.getDeltaTimeSeconds());
            model5.updatePosition(model1.getPosition().add(deltaPosition));

            renderer.prepare();
            shader.start();
            //renderer.render(model1);
            //renderer.render(model2);
            //renderer.render(model3);
            //renderer.render(model4);
            model5.render(renderer);
            shader.stop();

            if (pacer.getFramesElapsed() > TARGET_FPS) {
                System.out.printf("%.3f\n", pacer.getAverageFPS());
                pacer.resetFrameCounter();
            }

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

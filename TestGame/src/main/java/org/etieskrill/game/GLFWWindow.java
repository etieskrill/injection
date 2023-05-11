package org.etieskrill.game;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import jglm.Vec;
import org.etieskrill.engine.graphics.gl.*;
import org.etieskrill.engine.graphics.gl.shaders.ShaderFactory;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.math.Mat4f;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.math.Vec3f;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.ARBMatrixPalette;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.Platform;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.util.Arrays;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GLFWWindow {
    
    private static final double TARGET_FPS = 60d;

    private volatile boolean wPressed, aPressed, sPressed, dPressed;
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
        //GLUtil.setupDebugMessageCallback(System.out); //TODO unbind when done
        //GL33C.glViewport(0, 0, 1920, 1080); //this is apparently done ... somewhere behind the scenes?

        //Get max vertex attributes
        //int[] caps = new int[1];
        //GL30C.glGetIntegerv(GL30C.GL_MAX_VERTEX_ATTRIBS, caps);
        //System.out.println(Arrays.toString(caps));
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);

        if (!initTextureSettings()) throw new IllegalStateException("Could not initialise texture settings");
        if (!initKeybinds()) throw new IllegalStateException("Could not initialise keybinds");
        
        glfwShowWindow(window);
    }

    private boolean initTextureSettings() {
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_NEAREST); //GL_<mipmap level selection>_MIPMAP_<mipmap texture sampling>
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR); //GL_<mipmap texture sampling>
        return true;
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

        Vec3 cubePositions[] = {
            new Vec3( 0.0f,  0.0f,  0.0f),
            new Vec3( 2.0f,  5.0f, -15.0f),
            new Vec3(-1.5f, -2.2f, -2.5f),
            new Vec3(-3.8f, -2.0f, -12.3f),
            new Vec3( 2.4f, -0.4f, -3.5f),
            new Vec3(-1.7f,  3.0f, -7.5f),
            new Vec3( 1.3f, -2.0f, -2.5f),
            new Vec3( 1.5f,  2.0f, -2.5f),
            new Vec3( 1.5f,  0.2f, -1.5f),
            new Vec3(-1.3f,  1.0f, -1.5f)
        };

        RawModel[] models = new RawModel[cubePositions.length];
        for (int i = 0; i < cubePositions.length; i++) {
            models[i] = factory.box(new Vec3(0.1f, 0.1f, 0.1f))
                    .setPosition(cubePositions[i])
                    .setRotation(new Random(69420).nextFloat(),
                            Vec3.linearRand_(new Vec3(-1f, -1f, -1f), new Vec3(1f, 1f, 1f)));
            System.out.println(Arrays.toString(models[i].getTransform().toFa_()));
        }

        //GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        Texture containerTexture = new Texture("container.jpg", 0);
        //GL33C.glActiveTexture(GL33C.GL_TEXTURE1);
        Texture baleTexture = new Texture("buff_bale.jpg", 1);
        
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_MIRRORED_REPEAT);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_MIRRORED_REPEAT);
        
        Renderer renderer = new Renderer();
        ShaderProgram shader = ShaderFactory.getStandardShader();
        
        pacer.start();
        
        while (!glfwWindowShouldClose(window)) {
            Vec3 newPosition = new Vec3();
            if (wPressed) newPosition.set(newPosition.add(new Vec3(0f, 1f, 0f)));
            if (dPressed) newPosition.set(newPosition.add(new Vec3(1f, 0f, 0f)));
            if (sPressed) newPosition.set(newPosition.add(new Vec3(0f, -1f, 0f)));
            if (aPressed) newPosition.set(newPosition.add(new Vec3(-1f, 0f, 0f)));

            Vec3 deltaPosition = newPosition.mul((float) pacer.getDeltaTimeSeconds() * 0.5f);
            //model1.setPosition(model1.getPosition().add(deltaPosition));

            renderer.prepare();
            shader.start();

            float seconds = 0; //(float) pacer.getSecondsElapsedTotal();
            //shader.setUniformMat4("uModel", false, new Mat4().translate(0f, 0f, 5f).rotate(seconds, new Vec3(Math.sin(seconds), Math.cos(seconds), 0f).normalize()));
            shader.setUniformMat4("uView", false, new Mat4().translation(-0.0f, -0.0f, -0f));
            shader.setUniformMat4("uProjection", false, new Mat4().perspectiveFov((float) Math.toRadians(60f), 1920f, 1080f, 0.1f, 100f)); //the near fucking clipping plane needs to be positive in order for the z-buffer to work
            
            containerTexture.bind(0);
            baleTexture.bind(1);

            for (RawModel model : models) {
                shader.setUniformMat4("uModel", false, model.getTransform());
                renderer.render(model);
            }

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

package org.etieskrill.game;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.RawModel;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.Texture;
import org.etieskrill.engine.graphics.gl.shaders.ShaderFactory;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.scene._2d.Button;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.window.Window;
import org.etieskrill.engine.window.WindowBuilder;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;

import java.util.Arrays;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class DemCubez {
    
    private static final double TARGET_FPS = 60d;

    private volatile boolean wPressed, aPressed, sPressed, dPressed, spacePressed, shiftPressed;
    private volatile double pitch, yaw, roll, prevMouseX, prevMouseY, zoom;

    private Window window;
    
    public DemCubez() {
        init();
        loop();
        terminate();
    }
    
    private void init() {
        this.window = WindowBuilder.create()
                .setMode(Window.WindowMode.BORDERLESS)
                .setTitle("DemCubez")
                .build();

        if (!initGL()) throw new IllegalStateException("Could not initialise texture settings");
        if (!initKeybinds()) throw new IllegalStateException("Could not initialise keybinds");
        if (!initMouse()) throw new IllegalStateException("Could not initialise mouse");
        
        window.show();
    }

    private boolean initGL() {
        GL.createCapabilities();
        
        //GLUtil.setupDebugMessageCallback(System.out); //TODO unbind when done
        //GL33C.glViewport(0, 0, 1920, 1080); //this is apparently done ... somewhere behind the scenes?
    
        //Get max vertex attributes
        //int[] caps = new int[1];
        //GL30C.glGetIntegerv(GL30C.GL_MAX_VERTEX_ATTRIBS, caps);
        //System.out.println(Arrays.toString(caps));
        
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);
        GL33C.glDepthFunc(GL33C.GL_LESS);
        
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_NEAREST); //GL_<mipmap level selection>_MIPMAP_<mipmap texture sampling>
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR); //GL_<mipmap texture sampling>

        return GL33C.glGetError() == GL33C.GL_NO_ERROR;
    }
    
    private boolean initKeybinds() {
        glfwSetKeyCallback(window.getID(), (long window, int key, int scancode, int action, int mods) -> {
            switch (key) {
                case GLFW_KEY_ESCAPE -> {
                    if ((mods & GLFW_MOD_SHIFT) != 0)
                        glfwSetWindowShouldClose(window, true);
                }
                case GLFW_KEY_W -> wPressed = action != GLFW_RELEASE;
                case GLFW_KEY_A -> aPressed = action != GLFW_RELEASE;
                case GLFW_KEY_S -> sPressed = action != GLFW_RELEASE;
                case GLFW_KEY_D -> dPressed = action != GLFW_RELEASE;
                case GLFW_KEY_SPACE -> spacePressed = action != GLFW_RELEASE;
                case GLFW_KEY_LEFT_SHIFT -> shiftPressed = action != GLFW_RELEASE;
            }
        });
        
        return glfwGetError(null) == GLFW_NO_ERROR;
    }
    
    private boolean initMouse() {
        glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported())
            glfwSetInputMode(window.getID(), GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

        double[] prevX = new double[1], prevY = new double[1];
        glfwGetCursorPos(window.getID(), prevX, prevY);
        prevMouseX = prevX[0];
        prevMouseY = prevY[0];
        
        yaw = 90d;
        zoom = 3.81f; //this equates to almost 60° for the fov, don't ask why, i guessed it by trial and error
        
        float mouseSensitivity = 0.15f, zoomSensitivity = 0.5f;

        glfwSetCursorPosCallback(window.getID(), (window, xpos, ypos) -> {
            double dx = xpos - prevMouseX;
            double dy = ypos - prevMouseY;
            
            pitch += dy * mouseSensitivity;
            yaw -= dx * mouseSensitivity;
            
            if (pitch > 89d) {
                pitch = 89d;
            } else if (pitch < -89d) {
                pitch = -89d;
            }
            
            prevMouseX = xpos;
            prevMouseY = ypos;
        });

        glfwSetMouseButtonCallback(window.getID(), (window, button, action, mods) -> {
        });
        
        glfwSetScrollCallback(window.getID(), (window, xoffset, yoffset) -> {
            zoom -= yoffset * zoomSensitivity;
    
            if (zoom > 10d) {
                zoom = 10d;
            } else if (zoom < 0.1d) {
                zoom = 0.1d;
            }
        });
    
        return glfwGetError(null) == GLFW_NO_ERROR;
    }

    private void loop() {
        ModelFactory factory = new ModelFactory();

        Vec3[] cubePositions = {
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
            models[i] = factory.box(new Vec3(0.5f, 0.5f, 0.5f))
                    .setPosition(cubePositions[i])
                    .setRotation(new Random(69420).nextFloat(),
                            Vec3.linearRand_(new Vec3(-1f, -1f, -1f), new Vec3(1f, 1f, 1f)));
        }

        //GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        Texture containerTexture = new Texture("container.jpg", 0);
        //GL33C.glActiveTexture(GL33C.GL_TEXTURE1);
        Texture pepegaTexture = new Texture("pepega.png", 1);
        
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_MIRRORED_REPEAT);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_MIRRORED_REPEAT);
        
        Renderer renderer = new Renderer();
        ShaderProgram shader = ShaderFactory.getStandardShader();
    
        Vec3 camPosition = new Vec3(0f, 0f, -3f), camFront = new Vec3(0f, 0f, -1f), up = new Vec3(0f, 1f, 0f);
        
        window.setRoot(new Button(new Vec2f(100f, 100f), new Vec2f(50f, 10f), 0f));

        LoopPacer pacer = new SystemNanoTimePacer(1d / TARGET_FPS);
        pacer.start();


        while (!window.shouldClose()) {
            camFront.set(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)), Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
                    .normalize();
            Vec3 camRight = camFront.cross_(up).normalize(), camUp = camFront.cross_(camRight).normalize();
    
            Vec3 deltaPosition = new Vec3();
    
            float camSpeed = 2f;
            if (wPressed) add(deltaPosition, camFront);
            if (sPressed) add(deltaPosition, camFront.negate_());
            if (aPressed) add(deltaPosition, camRight);
            if (dPressed) add(deltaPosition, camRight.negate_());
            if (spacePressed) add(deltaPosition, camUp);
            if (shiftPressed) add(deltaPosition, camUp.negate_());
            
            float delta = (float) pacer.getDeltaTimeSeconds();
            deltaPosition.set(deltaPosition.x * delta * camSpeed, deltaPosition.y * delta * camSpeed, deltaPosition.z * delta * camSpeed);
            add(camPosition, deltaPosition);
            
            renderer.prepare();
            shader.start();

            Vec3 pos = new Vec3(camPosition), target = add(new Vec3(pos), camFront);
            Mat4 view = new Mat4().lookAt(pos, target, up).translate(new Vec3(pos.x * 2, pos.y * 2, pos.z * 2));
            shader.setUniformMat4("uView", false, view);
            
            float fov = (float) (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
            Mat4 clip = new Mat4().perspectiveFov((float) Math.toRadians(fov), window.getSize().getWidth(), window.getSize().getHeight(), 0.1f, 100f);
            //clip.set(clip.ortho(0f, window.getSize().getWidth() * 2f, 0f, window.getSize().getHeight() * 2f, 0f, -100f));
            clip.set(clip.ortho(-(float) zoom, (float) zoom, -(float) zoom / window.getSize().getAspectRatio(), (float) (zoom / window.getSize().getAspectRatio()), 0f, -100f));
            shader.setUniformMat4("uProjection", false, clip); //the near fucking clipping plane needs to be positive in order for the z-buffer to work
            
            containerTexture.bind(0);
            pepegaTexture.bind(1);

            for (RawModel model : models) {
                shader.setUniformMat4("uModel", false, model.getTransform());
                renderer.render(model);
            }

            shader.stop();

            if (pacer.getFramesElapsed() > TARGET_FPS) {
                System.out.printf("%.3f\n", pacer.getAverageFPS());
                pacer.resetFrameCounter();
            }

            view = new Mat4().identity();
            shader.setUniformMat4("uView", false, view);

            clip = new Mat4().identity();
            shader.setUniformMat4("uProjection", false, clip);

            RawModel rect = factory.rectangle(0f, 0f, 0.5f, 1f).setPosition(new Vec3(0f, 0f, 10f));
            shader.setUniformMat4("uModel", false, rect.getTransform());
            renderer.render(rect);
            window.update(renderer, factory);
            renderer.render(rect);
            pacer.nextFrame();
        }
        
        shader.dispose();
        factory.disposeLoader();
    }
    
    private void terminate() {
        GL33C.glFlush();
        glfwTerminate();
    }
    
    private static Vec3 add(Vec3 a, Vec3 b) {
        return a.set(a.x + b.x, a.y + b.y, a.z + b.z);
    }
    
    private static Vec3 mul(Vec3 a, Vec3 b) {
        return a.set(a.x * b.x, a.y * b.y, a.z * b.z);
    }
    
    private String matToString(Mat4 mat) {
        return String.format("""
                        [%6.3f, %6.3f, %6.3f, %6.3f]
                        [%6.3f, %6.3f, %6.3f, %6.3f]
                        [%6.3f, %6.3f, %6.3f, %6.3f]
                        [%6.3f, %6.3f, %6.3f, %6.3f]""",
                mat.m00, mat.m01, mat.m02, mat.m03,
                mat.m10, mat.m11, mat.m12, mat.m13,
                mat.m20, mat.m21, mat.m22, mat.m23,
                mat.m30, mat.m31, mat.m32, mat.m33);
    }
    
    public static void main(String[] args) {
        new DemCubez();
    }
    
}

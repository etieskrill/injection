package org.etieskrill.game;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import org.etieskrill.engine.graphics.OrthographicCamera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.*;
import org.etieskrill.engine.graphics.gl.shaders.ShaderFactory;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.scene._2d.*;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.window.Window;
import org.etieskrill.engine.window.WindowBuilder;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;

import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class DemCubez {
    
    private static final float TARGET_FPS = 60f;

    private volatile boolean wPressed, aPressed, sPressed, dPressed, spacePressed, shiftPressed, qPressed, ePressed,
            escPressed, escPressedPrev, ctrlPressed;
    private volatile boolean paused;
    private volatile double dPitch, dYaw, dRoll, prevMouseX, prevMouseY, zoom;

    private Window window;
    
    public DemCubez() {
        init();
        loop();
        terminate();
    }
    
    private void init() {
        this.window = WindowBuilder.create()
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(TARGET_FPS)
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
        /*int[] caps = new int[1];
        GL33C.glGetIntegerv(GL33C.GL_MAX_VERTEX_ATTRIBS, caps);
        System.out.println(Arrays.toString(caps));
        GL33C.glGetIntegerv(GL33C.GL_MAX_TEXTURE_IMAGE_UNITS, caps);
        System.out.println(Arrays.toString(caps));*/
        
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);
        GL33C.glDepthFunc(GL33C.GL_LESS);
        
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_NEAREST); //GL_<mipmap level selection>_MIPMAP_<mipmap texture sampling>
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR); //GL_<mipmap texture sampling>
    
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_MIRRORED_REPEAT);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_MIRRORED_REPEAT);

        return GL33C.glGetError() == GL33C.GL_NO_ERROR;
    }
    
    private boolean initKeybinds() {
        glfwSetKeyCallback(window.getID(), (long window, int key, int scancode, int action, int mods) -> {
            switch (key) {
                case GLFW_KEY_ESCAPE -> {
                    if (mods == 0 && action == GLFW_RELEASE) {
                        escPressed = !escPressed;
                    }
                    else if ((mods & GLFW_MOD_SHIFT) != 0)
                        glfwSetWindowShouldClose(window, true);
                }
                case GLFW_KEY_W -> wPressed = action != GLFW_RELEASE;
                case GLFW_KEY_A -> aPressed = action != GLFW_RELEASE;
                case GLFW_KEY_S -> sPressed = action != GLFW_RELEASE;
                case GLFW_KEY_D -> dPressed = action != GLFW_RELEASE;
                case GLFW_KEY_SPACE -> spacePressed = action != GLFW_RELEASE;
                case GLFW_KEY_LEFT_SHIFT -> shiftPressed = action != GLFW_RELEASE;
                case GLFW_KEY_Q -> qPressed = action != GLFW_RELEASE;
                case GLFW_KEY_E -> ePressed = action != GLFW_RELEASE;
                case GLFW_KEY_LEFT_CONTROL -> ctrlPressed = action != GLFW_RELEASE;
            }
        });
        
        return glfwGetError(null) == GLFW_NO_ERROR;
    }
    
    private boolean initMouse() {
        glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        //if (glfwRawMouseMotionSupported())
        //    glfwSetInputMode(window.getID(), GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        
        resetPreviousMousePosition();
        
        float mouseSensitivity = 0.25f, zoomSensitivity = 0.5f;

        glfwSetCursorPosCallback(window.getID(), (window, xpos, ypos) -> {
            if (paused) return;

            double dx = ((xpos - prevMouseX) * mouseSensitivity);
            double dy = -((ypos - prevMouseY) * mouseSensitivity);
            
            dPitch = (float) dy;
            dYaw = (float) dx;
            
            //pitch += Math.cos(roll) * dy + Math.sin(roll) * dx;
            //yaw -= Math.cos(roll) * dx + Math.sin(roll) * dy;
            
//            if (pitch > 89f) {
//                pitch = 89f;
//            } else if (pitch < -89f) {
//                pitch = -89f;
//            }
            
            prevMouseX = (float) xpos;
            prevMouseY = (float) ypos;
        });

        glfwSetMouseButtonCallback(window.getID(), (window, button, action, mods) -> {
        });
        
        glfwSetScrollCallback(window.getID(), (window, xoffset, yoffset) -> {
            zoom -= yoffset * zoomSensitivity;
    
            if (zoom > 10d) {
                zoom = 10f;
            } else if (zoom < 0.1d) {
                zoom = 0.1f;
            }
        });
    
        return glfwGetError(null) == GLFW_NO_ERROR;
    }
    
    private void resetPreviousMousePosition() {
        double[] prevX = new double[1], prevY = new double[1];
        glfwGetCursorPos(window.getID(), prevX, prevY);
        prevMouseX = (float) prevX[0];
        prevMouseY = (float) prevY[0];
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
    
        Texture containerTexture = new Texture("container2.png");
        Texture containerSpecular = new Texture("container2_specular.png");
        Texture containerEmissive = new Texture("container2_emissive.jpg");
        Texture pepegaTexture = new Texture("pepega.png");
        
        Model[] models = new Model[cubePositions.length];
        for (int i = 0; i < cubePositions.length; i++) {
            models[i] = new Model(factory.box(new Vec3(0.5f, 0.5f, 0.5f))
                    .setPosition(cubePositions[i])
                    .setRotation(new Random(69420).nextFloat(),
                            Vec3.linearRand_(new Vec3(-1f, -1f, -1f), new Vec3(1f, 1f, 1f))));
            models[i].addTexture(containerTexture, 0).addTexture(containerSpecular, 1)
                    .addTexture(containerEmissive, 2);
        }

        RawModel lightSource = factory
                .box(new Vec3(0.2f, 0.2f, 0.2f))
                .setPosition(new Vec3(0f, 0f, -5f));
        
        Renderer renderer = new Renderer();
        ShaderProgram shader = ShaderFactory.getStandardShader();
        ShaderProgram lightShader = ShaderFactory.getLightSourceShader();
        
        Batch batch = new Batch(renderer, factory);
    
        //Vec3 camPosition = new Vec3(0f, 0f, -3f), camFront = new Vec3(0f, 0f, -1f), up = new Vec3(0f, 1f, 0f);
        PerspectiveCamera camera = new PerspectiveCamera(window.getSize().getVector());
    
        camera.setPosition(new Vec3(0f, 0f, -3f))
                .setOrientation(0f, 90f, 0f)
                .setZoom(3.81f); //this equates to almost 60° for the fov, don't ask why, i guessed it by trial and error

        zoom = camera.getZoom();
        
        Container container = new Container();
        container.getLayout().setAlignment(Layout.Alignment.TOP_LEFT);
        Layout layout = Layout.get()
                .setPrefSize(new Vec2f(400f, 100f))
                .setMinSize(new Vec2f(250f, 50f));
        Button button1 = new Button(layout);
        Button button2 = new Button(layout);
        Button button3 = new Button(layout);
        VerticalGroup menu = new VerticalGroup(button1, button2, button3);
        //menu.layout();
        //menu.getLayout().setAlignment(Layout.Alignment.CENTER);
        container.setChild(menu);

        window.setStage(new Stage(batch, container, new OrthographicCamera(window.getSize().getVector())));
        window.getStage().hide();

        LoopPacer pacer = new SystemNanoTimePacer(1d / TARGET_FPS);
        pacer.start();
        
        while (!window.shouldClose()) {
            if (escPressed && !escPressedPrev) {
                paused = true;
                pacer.pauseTimer();
                glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                window.getStage().show();
                escPressedPrev = true;
            }
            else if (!escPressed && escPressedPrev) {
                paused = false;
                pacer.resumeTimer();
                resetPreviousMousePosition();
                glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                window.getStage().hide();
                escPressedPrev = false;
            }
    
            if (!escPressed) {
                camera.orient(dPitch, dYaw, 0f);
                dPitch = 0f;
                dYaw = 0f;
            }
    
            Vec3 deltaPosition = new Vec3();
    
            float camSpeed = !ctrlPressed ? 2f : 4f;
            if (wPressed) add(deltaPosition, new Vec3(0f, 0f, 1f));
            if (sPressed) add(deltaPosition, new Vec3(0f, 0f, -1f));
            if (aPressed) add(deltaPosition, new Vec3(-1f, 0f, 0f));
            if (dPressed) add(deltaPosition, new Vec3(1f, 0f, 0f));
            if (spacePressed) add(deltaPosition, new Vec3(0f, -1f, 0f));
            if (shiftPressed) add(deltaPosition, new Vec3(0f, 1f, 0f));
    
            float delta = (float) pacer.getDeltaTimeSeconds();
            deltaPosition = mul_(mul_(deltaPosition, camSpeed), delta);
            if (!escPressed) camera.translate(deltaPosition);

            float camRollSpeed = 1f, camRoll = 0;
            if (qPressed) camRoll -= camRollSpeed;
            if (ePressed) camRoll += camRollSpeed;
            
            dRoll = camRoll % 360;
            //up.set(new Mat3().rotateZ(Math.toRadians(roll)).mul(new Vec3(0f, 1f, 0f)));

            camera.setZoom((float) zoom);

            double radius = 6.5f, speed = 50f, time = speed * pacer.getTime();
            Vec3 newLightSourcePos = new Vec3(radius * Math.cos(Math.toRadians(time)), 0f, radius * Math.sin(Math.toRadians(time)));
            if (!escPressed) lightSource.setPosition(newLightSourcePos);
            
            renderer.prepare();
            shader.start();

            camera.update();
            shader.setUniformMat4("uView", false, camera.getView());
            shader.setUniformMat4("uProjection", false, camera.getPerspective());

            shader.setUniformVec3("light.position", lightSource.getPosition());
            
            //These are essentially intensity factors
            Vec3 lightColour = new Vec3(1f);
            Vec3 ambient = mul_(lightColour, 0.2f);
            shader.setUniformVec3("light.ambient", ambient);
            Vec3 diffuse = mul_(lightColour, 0.5f);
            shader.setUniformVec3("light.diffuse", diffuse);
            Vec3 specular = mul_(lightColour, 1f);
            shader.setUniformVec3("light.specular", specular);

            shader.setUniformFloat("light.constant", 1f);
            shader.setUniformFloat("light.linear", 0.01f);
            shader.setUniformFloat("light.quadratic", 0.005f);
            
            //shader.setUniformVec3("flashlight.position", camPosition);
            //shader.setUniformVec3("flashlight.direction", camFront);
            //shader.setUniformFloat("flashlight.cutoff", (float) Math.cos(Math.toRadians(12.5)));
            
            //shader.setUniformVec3("flashlight.ambient", ambient);
            //shader.setUniformVec3("flashlight.diffuse", diffuse);
            //shader.setUniformVec3("flashlight.specular", specular);
    
            //shader.setUniformFloat("flashlight.constant", 1f);
            //shader.setUniformFloat("flashlight.linear", 0.09f);
            //shader.setUniformFloat("flashlight.quadratic", 0.032f);
            
            shader.setUniformVec3("uViewPosition", camera.getPosition());
            shader.setUniformFloat("uTime", (float) pacer.getTime());
            
            //Bind material struct to samplers and assign values
            shader.setUniformInt("material.diffuse", 0); //TODO automating this would require a model- and shader-dependent object
            shader.setUniformInt("material.specular", 1);
            shader.setUniformInt("material.emission", 2);
            shader.setUniformFloat("material.shininess", 64);

            for (Model model : models) {
                shader.setUniformMat4("uModel", false, model.getTransform());
                shader.setUniformMat3("uNormal", false, model.getTransform()
                        .inverse_().transpose().toMat3_());
                renderer.render(model);
            }
    
            //System.out.println("main: " + batch);
            //button.render(batch);
            
            lightShader.start();
            lightShader.setUniformMat4("uCombined", false, camera.getCombined());
            lightShader.setUniformMat4("uModel", false, lightSource.getTransform());
            
            lightShader.setUniformVec3("light.ambient", ambient);
            lightShader.setUniformVec3("light.diffuse", diffuse);
            lightShader.setUniformVec3("light.specular", specular);

            renderer.render(lightSource);

            shader.start();

            /*Mat4 view = new Mat4().identity();
            shader.setUniformMat4("uView", false, view);

            Mat4 clip = new Mat4().identity();
            shader.setUniformMat4("uProjection", false, clip);

            shader.setUniformMat4("uModel", false, new Mat4());
             */
    
            //button.draw(batch);
            
            window.update(pacer.getDeltaTimeSeconds());
            
            shader.stop();

            if (pacer.getFramesElapsed() > TARGET_FPS) {
                System.out.printf("%.3f\n", pacer.getAverageFPS());
                pacer.resetFrameCounter();
            }
            
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
    
    private static Vec3 mul_(Vec3 a, float s) {
        return new Vec3(a.x * s, a.y * s, a.z * s);
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

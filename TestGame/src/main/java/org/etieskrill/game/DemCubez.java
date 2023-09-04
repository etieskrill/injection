package org.etieskrill.game;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.OrthographicCamera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.Loaders.ModelLoader;
import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.scene._2d.*;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.window.Window;
import org.etieskrill.engine.window.Window.Cursor.CursorMode;
import org.etieskrill.engine.window.WindowBuilder;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public class DemCubez {
    
    private static final float TARGET_FPS = 60f;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModelLoader modelLoader = new ModelLoader();
    
    private volatile boolean wPressed, aPressed, sPressed, dPressed, spacePressed, shiftPressed, qPressed, ePressed,
            escPressed, escPressedPrev, ctrlPressed;
    private volatile boolean paused;
    private volatile double dPitch, dYaw, dRoll, prevMouseX, prevMouseY;

    private Window window;
    
    private PerspectiveCamera camera;
    private LoopPacer pacer;
    
    ShaderProgram containerShader;
    ShaderProgram lightShader;
    ShaderProgram swordShader;
    ShaderProgram backpackShader;
    
    Model[] models;
    Vector<Model> grassModels;
    Model[] lightSources;
    Model sword;
    Model backpack;
    
    Renderer renderer;
    
    public DemCubez() {
        init();
        loop();
        terminate();
    }
    
    private void init() {
        Window.USE_RAW_MOUSE_MOTION_IF_AVAILABLE = false;
        
        this.window = WindowBuilder.create()
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(TARGET_FPS)
                .setTitle("DemCubez")
                .build();
        logger.info("Window initialised");

        if (!initGL()) throw new IllegalStateException("Could not initialise graphics settings");
        if (!initKeybinds()) throw new IllegalStateException("Could not initialise keybinds");
        if (!initMouse()) throw new IllegalStateException("Could not initialise mouse");
    
        loadModels();
        loadShaders();
    
        renderer = new Renderer();
        camera = (PerspectiveCamera) new PerspectiveCamera(window.getSize().getVector())
                .setPosition(new Vec3(0f, 0f, 3f))
                .setOrientation(0f, -90f, 0f)
                .setFar(-1000f);
        
        initUI();
        window.show();
    }

    private boolean initGL() {
        GL.createCapabilities();
        
//        GLUtil.setupDebugMessageCallback(System.out); //TODO unbind when done
//        GL33C.glViewport(0, 0, 1920, 1080); //this is apparently done ... somewhere behind the scenes?
//
        //Get max vertex attributes
//        System.out.println(glGetInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS));
//        System.out.println(glGetInteger(GL_MAX_VARYING_FLOATS));
//        System.out.println(glGetInteger(GL_MAX_VERTEX_ATTRIBS));
//        System.out.println(glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));
//        System.out.println(glGetInteger(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS));
//        System.out.println(glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS));
        
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_BLEND);
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST); //GL_<mipmap level selection>_MIPMAP_<mipmap texture sampling>
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); //GL_<mipmap texture sampling>
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        return glGetError() == GL_NO_ERROR;
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
        window.getCursor().setMode(CursorMode.DISABLED);
        
        resetPreviousMousePosition();
        
        float mouseSensitivity = 0.15f, zoomSensitivity = 0.5f;

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
            double zoom = camera.getZoom() - yoffset * zoomSensitivity;
    
            if (zoom > 10d) {
                zoom = 10f;
            } else if (zoom < 0.1d) {
                zoom = 0.1f;
            }
            
            camera.setZoom((float) zoom);
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
        pacer = new SystemNanoTimePacer(1d / TARGET_FPS);
        pacer.start();
        
        while (!window.shouldClose()) {
            //Toggle escape button and related behaviour
            if (escPressed && !escPressedPrev) {
                paused = true;
                pacer.pauseTimer();
                window.getCursor().normal();
                window.getStage().show();
                escPressedPrev = true;
            }
            else if (!escPressed && escPressedPrev) {
                paused = false;
                pacer.resumeTimer();
                resetPreviousMousePosition();
                window.getCursor().disable();
                window.getStage().hide();
                escPressedPrev = false;
            }
    
            updatePlayerCamera();
    
            String timing = "";
            long time = 0;
            if (pacer.getFramesElapsed() > TARGET_FPS) {
                time = System.nanoTime();
                updateModels();
                timing = ", update: %d ys,".formatted(System.nanoTime() - time);
                
                time = System.nanoTime();
                renderModels();
                timing += " render: %d ys".formatted(System.nanoTime() - time);
            } else {
                updateModels();
                renderModels();
            }
            
            window.update(pacer.getDeltaTimeSeconds());
            
            if (pacer.getFramesElapsed() > TARGET_FPS) {
                timing = "fps: %.3f".formatted(pacer.getAverageFPS()) + timing;
                System.out.println(timing);
                pacer.resetFrameCounter();
            }
            
            pacer.nextFrame();
        }
    }
    
    private void updatePlayerCamera() {
        if (!paused) {
            camera.orient(dPitch, dYaw, 0f);
            dPitch = 0f;
            dYaw = 0f;
        }
        
        Vec3 deltaPosition = new Vec3();
    
        float camSpeed = !ctrlPressed ? 2f : 4f;
        if (wPressed) deltaPosition.plusAssign(new Vec3(0f, 0f, 1f));
        if (sPressed) deltaPosition.plusAssign(new Vec3(0f, 0f, -1f));
        if (aPressed) deltaPosition.plusAssign(new Vec3(-1f, 0f, 0f));
        if (dPressed) deltaPosition.plusAssign(new Vec3(1f, 0f, 0f));
        if (spacePressed) deltaPosition.plusAssign(new Vec3(0f, -1f, 0f));
        if (shiftPressed) deltaPosition.plusAssign(new Vec3(0f, 1f, 0f));
    
        deltaPosition.normalize();
    
        float delta = (float) pacer.getDeltaTimeSeconds();
        deltaPosition = deltaPosition.times(camSpeed).times(delta);
        if (!paused) camera.translate(deltaPosition);
    
        float camRollSpeed = 1f, camRoll = 0;
        if (qPressed) camRoll -= camRollSpeed;
        if (ePressed) camRoll += camRollSpeed;
    
        dRoll = camRoll % 360;
        //up.set(new Mat3().rotateZ(Math.toRadians(roll)).mul(new Vec3(0f, 1f, 0f)));
    }
    
    private void loadModels() {
        Vec3[] cubePositions = {
                //new Vec3( 0.0f,  0.0f,  0.0f),
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
    
        models = new Model[cubePositions.length];
        Random random = new Random(69420);
        ModelLoader.getInstance().load("cube", () -> Model.ofFile("cube.obj"));
        for (int i = 0; i < cubePositions.length; i++) {
            models[i] = ModelLoader.getInstance().get("cube")
                    .setPosition(cubePositions[i])
                    .setRotation(random.nextFloat(),
                            new Vec3(
                                    random.nextFloat(-1f, 1f),
                                    random.nextFloat(-1f, 1f),
                                    random.nextFloat(-1f, 1f))
                    );
        }
    
        Vec3[] grassPosition = {
                new Vec3(-1.5f,  0.0f, -0.48f),
                new Vec3( 1.5f,  0.0f,  0.51f),
                new Vec3( 0.0f,  0.0f,  0.7f),
                new Vec3(-0.3f,  0.0f, -2.3f),
                new Vec3( 0.5f,  0.0f, -0.6f)
        };
        grassModels = new Vector<>(grassPosition.length);
        for (Vec3 position : grassPosition) {
            grassModels.add(ModelLoader.getInstance().load("grass", () ->
                            new Model.Builder("grass.obj").disableCulling().hasTransparency().build())
                    .setPosition(position)
                    .setRotation((float) Math.toRadians(180f), new Vec3(0f, 0f, 1f))
            );
        }
    
        lightSources = new Model[2];
        for (int i = 0; i < lightSources.length; i++) {
            lightSources[i] = ModelLoader.getInstance().load("light", () ->
                            new Model.Builder("cube.obj").setName("light").build())
                    .setScale(0.2f)
                    .setPosition(new Vec3(0f, 0f, -5f));
        }
    
        sword = ModelLoader.getInstance().load("sword", () ->
                Model.ofFile("Sting-Sword.obj")
                        .setPosition(new Vec3(0, 0, -2))
                        .setScale(new Vec3(0.1f))
                        .setRotation((float) Math.toRadians(90f), new Vec3(1f, 0f, 0f))
        );
    
        backpack = ModelLoader.getInstance().load("backpack", () ->
                Model.ofFile("backpack.obj")
                        .setPosition(new Vec3(-3, -1, -2))
                        .setScale(new Vec3(0.15f))
                        .setRotation((float) Math.toRadians(180f), new Vec3(1f, 0f, 1f))
        );
    }
    
    private void loadShaders() {
        containerShader = Shaders.getContainerShader();
        lightShader = Shaders.getLightSourceShader();
        swordShader = Shaders.getSwordShader();
        backpackShader = Shaders.getBackpackShader();
    }
    
    private void initUI() {
        Container container = new Container();
        //container.getLayout().setAlignment(Layout.Alignment.BOTTOM_RIGHT);
        Layout layout = Layout.get()
                .setPrefSize(new Vec2(400f, 100f))
                .setMinSize(new Vec2(250f, 50f));
        Button button1 = new Button(layout);
        Button button2 = new Button(layout);
        Button button3 = new Button(layout);
        VerticalGroup menu = new VerticalGroup(button1, button2, button3);
        //menu.layout();
        //menu.getLayout().setAlignment(Layout.Alignment.CENTER);
        container.setChild(menu);
    
        window.setStage(new Stage(new Batch(renderer, new ModelFactory()), container, new OrthographicCamera(window.getSize().getVector())));
        window.getStage().hide();
    }
    
    private void setShaderUniforms() {
        ShaderProgram[] doLighting = {containerShader, swordShader, backpackShader};
        
        Vec3 globalLightDirection = new Vec3(1f);
    
        //These are essentially intensity factors
        //TODO isn't this kind of stupid? why would a light SOURCE have different kinds of intensities?
        Vec3 lightColour = new Vec3(1f);
        Vec3 ambient = lightColour.times(0.1f);
        Vec3 diffuse = lightColour.times(0.25f);
        Vec3 specular = lightColour.times(0.5f);
    
        for (ShaderProgram shader : doLighting) {
            shader.setUniformArray("globalLights[$].direction", 0, globalLightDirection);
            shader.setUniformArray("globalLights[$].ambient", 0, ambient);
            shader.setUniformArray("globalLights[$].diffuse", 0, diffuse);
            shader.setUniformArray("globalLights[$].specular", 0, specular);
        }
    
        Vec3 pointLightColour = new Vec3(1f);
        Vec3 pointLightAmbient = pointLightColour.times(0.1f);
        Vec3 pointLightDiffuse = pointLightColour.times(0.3f);
        Vec3 pointLightSpecular = pointLightColour.times(0.5f);
    
        for (ShaderProgram shader : doLighting) {
            for (int i = 0; i < lightSources.length; i++) {
                shader.setUniformArray("lights[$].position", i, lightSources[i].getPosition());
                shader.setUniformArray("lights[$].ambient", i, pointLightAmbient);
                shader.setUniformArray("lights[$].diffuse", i, pointLightDiffuse);
                shader.setUniformArray("lights[$].specular", i, pointLightSpecular);
                shader.setUniformArray("lights[$].constant", i, 1f);
                shader.setUniformArray("lights[$].linear", i, 0.01f);
                shader.setUniformArray("lights[$].quadratic", i, 0.005f);
            }
        }
    
        //TODO consider passing fragment position to frag shader with view applied,
        // so this nonsense becomes unnecessary
        containerShader.setUniform("uViewPosition", camera.getPosition());
        containerShader.setUniform("uTime", (float) pacer.getTime());
    
        swordShader.setUniform("uViewPosition", camera.getPosition());
        swordShader.setUniform("uTime", (float) pacer.getTime());
    
        backpackShader.setUniform("uViewPosition", camera.getPosition());
    
        lightShader.setUniform("light.ambient", pointLightAmbient);
        lightShader.setUniform("light.diffuse", pointLightDiffuse);
        lightShader.setUniform("light.specular", pointLightSpecular);
    }
    
    private void updateModels() {
        double radius = 4f, speed = 50f, time = speed * pacer.getTime();
        Vec3 offset = new Vec3(0f, 0f, -2f);
        Vec3 newLightSourcePos = new Vec3(
                radius * Math.cos(Math.toRadians(time)),
                0f,
                radius * Math.sin(Math.toRadians(time))
        );
        if (!paused) {
            lightSources[0].setPosition(newLightSourcePos.plus(offset));
            lightSources[1].setPosition(newLightSourcePos.negate().plus(offset));
        }
    
        if (!paused) {
            for (Model cube : models)
                cube.setRotation(cube.getRotation() + 0.01f, cube.getRotationAxis());
        }
    }
    
    private void renderModels() {
        renderer.prepare();
    
        setShaderUniforms();
    
        for (Model model : models)
            renderer.render(model, containerShader, camera.getCombined());
    
        renderer.render(sword, swordShader, camera.getCombined());
    
        //TODO this is gonna look weird no matter what, since here a phong shader attempts to render a pbr model
        renderer.render(backpack, backpackShader, camera.getCombined());
    
        for (int i = 0; i < lightSources.length; i++) {
        
            renderer.render(lightSources[i], lightShader, camera.getCombined());
        }
    
        //TODO 1. draw opaque, 2. sort transparent by decreasing distance to viewer, 3. draw sorted transparent
        grassModels.sort((model1, model2) -> Float.compare(
                camera.getPosition().minus(model2.getPosition()).length(),
                camera.getPosition().minus(model1.getPosition()).length())
        );
        for (Model grass : grassModels) {
            renderer.render(grass, containerShader, camera.getCombined());
        }
    }
    
    private void terminate() {
        containerShader.dispose();
        swordShader.dispose();
        backpackShader.dispose();
        lightShader.dispose();
        modelLoader.dispose();
        
        glFlush();
        glfwTerminate();
    }
    
    public static void main(String[] args) {
        new DemCubez();
    }
    
}

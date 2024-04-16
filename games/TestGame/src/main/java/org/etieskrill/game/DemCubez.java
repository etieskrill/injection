package org.etieskrill.game;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.model.CubeMapModel;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.input.CursorInputAdapter;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders.ModelLoader;
import org.etieskrill.engine.window.Cursor.CursorMode;
import org.etieskrill.engine.window.Window;
import org.joml.*;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    ShaderProgram skyboxShader;
    
    CubeMapModel skybox;
    Model[] models;
    List<Model> grassModels;
    Model[] lightSources;
    DirectionalLight globalLight;
    PointLight[] lights;
    Model sword;
    Model backpack;
    
    Renderer renderer;
    
    public DemCubez() {
        init();
        loop();
        terminate();
    }
    
    private void init() {
//        if (true) throw new RuntimeException("Will compile and run, but is currently broken"); //TODO fix

        Window.USE_RAW_MOUSE_MOTION_IF_AVAILABLE = false;
        
        this.window = new Window.Builder()
                .setMode(Window.WindowMode.BORDERLESS)
                .setRefreshRate(TARGET_FPS)
                .setTitle("DemCubez")
                .build();
        logger.info("Window initialised");
        
        glfwSetErrorCallback(((error, description) -> logger.warn("GLFW error {}: {}", error, description)));
        
        if (!initGL()) throw new IllegalStateException("Could not initialise graphics settings");
        if (!initKeybinds()) throw new IllegalStateException("Could not initialise keybinds");
        if (!initMouse()) throw new IllegalStateException("Could not initialise mouse");
    
        loadModels();
        loadShaders();

        renderer = new GLRenderer();
        camera = (PerspectiveCamera) new PerspectiveCamera(window.getSize().toVec())
                .setPosition(new Vector3f(0f, 0f, 3f))
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

        return glGetError() == GL_NO_ERROR;
    }
    
    private boolean initKeybinds() {
        window.addKeyInputs((type, key, action, modifiers) -> {
            switch (key) {
                case GLFW_KEY_ESCAPE -> {
                    if (modifiers == 0 && action == GLFW_RELEASE) escPressed = !escPressed;
                    else if ((modifiers & GLFW_MOD_SHIFT) != 0) window.close();
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
            return true;
        });
        
        return glfwGetError(null) == GLFW_NO_ERROR;
    }
    
    private boolean initMouse() {
        window.getCursor().setMode(CursorMode.DISABLED);
        
        resetPreviousMousePosition();
        
        float mouseSensitivity = 0.15f, zoomSensitivity = 0.5f;
        window.addCursorInputs(new CursorInputAdapter() {
            @Override
            public boolean invokeMove(double posX, double posY) {
                if (paused) return false;

                double dx = -((posX - prevMouseX) * mouseSensitivity);
                double dy = ((posY - prevMouseY) * mouseSensitivity);

                dPitch = (float) dy;
                dYaw = (float) dx;

                prevMouseX = (float) posX;
                prevMouseY = (float) posY;

                return true;
            }

            @Override
            public boolean invokeScroll(double deltaX, double deltaY) {
                double zoom = camera.getZoom() - deltaY * zoomSensitivity;
                camera.setZoom((float) zoom);
                return true;
            }
        });
    
        return glfwGetError(null) == GLFW_NO_ERROR;
    }
    
    private void resetPreviousMousePosition() {
        Vector2d mousePos = window.getCursor().getPosition();
        prevMouseX = (float) mousePos.x();
        prevMouseY = (float) mousePos.y();
    }

    private void loop() {
        pacer = new SystemNanoTimePacer(1d / TARGET_FPS);
        pacer.start();
        
        FrameBuffer frameBuffer = FrameBuffer.getStandard(
                new Vector2i(window.getSize().toVec().get(RoundingMode.TRUNCATE, new Vector2i())));
        frameBuffer.unbind();
        
        ShaderProgram screenShader = Shaders.getPostprocessingShader();

        FrameBufferAttachment attachment = frameBuffer.getAttachments().get(BufferAttachmentType.COLOUR0);
        Texture2D textureBuffer = (Texture2D) attachment;
        textureBuffer.bind(0);
    
        Material mat = new Material.Builder().addTextures(textureBuffer).build();
        Model screenQuad = ModelFactory.rectangle(-1, -1, 2, 2, mat).build();
        
        while (!window.shouldClose()) {
            //Toggle escape button and related behaviour
            if (escPressed && !escPressedPrev) {
                paused = true;
                pacer.pauseTimer();
                window.getCursor().enable();
                window.getScene().show();
                escPressedPrev = true;
            }
            else if (!escPressed && escPressedPrev) {
                paused = false;
                pacer.resumeTimer();
                resetPreviousMousePosition();
                window.getCursor().disable();
                window.getScene().hide();
                escPressedPrev = false;
            }
    
            updatePlayerCamera();
            
            updateModels();
            
//            frameBuffer.bind();
            renderModels();
//            frameBuffer.unbind();
            
//            renderer.prepare();
//            screenShader.start();
//            screenShader.setUniform("uSharpen", true);
//            screenShader.setUniform("uSharpenOffset", 1f / 10000f);

//            screenShader.setUniform("uColour", new Vector3f(1.0f));
            glEnable(GL_FRAMEBUFFER_SRGB); //manual gamma correction for funsies
//            renderer.render(screenQuad, screenShader, new Matrix4f());
            glDisable(GL_FRAMEBUFFER_SRGB);

            window.update(pacer.getDeltaTimeSeconds());
            
            if (pacer.getFramesElapsed() > TARGET_FPS) {
                String fpsString = "%.3f".formatted(pacer.getAverageFPS());
                logger.info("Current fps: {}", fpsString);
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
        
        Vector3f deltaPosition = new Vector3f();
    
        float camSpeed = !ctrlPressed ? 2f : 4f;
        if (wPressed) deltaPosition.add(new Vector3f(0f, 0f, 1f));
        if (sPressed) deltaPosition.add(new Vector3f(0f, 0f, -1f));
        if (aPressed) deltaPosition.add(new Vector3f(-1f, 0f, 0f));
        if (dPressed) deltaPosition.add(new Vector3f(1f, 0f, 0f));
        if (spacePressed) deltaPosition.add(new Vector3f(0f, -1f, 0f));
        if (shiftPressed) deltaPosition.add(new Vector3f(0f, 1f, 0f));
    
        deltaPosition.normalize();
    
        float delta = (float) pacer.getDeltaTimeSeconds();
        deltaPosition.mul(camSpeed).mul(delta);
        if (!paused) camera.translate(deltaPosition);
    
        float camRollSpeed = 1f, camRoll = 0;
        if (qPressed) camRoll -= camRollSpeed;
        if (ePressed) camRoll += camRollSpeed;
    
        dRoll = camRoll % 360;
        //up.set(new Mat3().rotateZ(Math.toRadians(roll)).mul(new Vector3f(0f, 1f, 0f)));
    }
    
    private void loadModels() {
        skybox = new CubeMapModel("space");
        
        Vector3f[] cubePositions = {
                //new Vector3f( 0.0f,  0.0f,  0.0f),
                new Vector3f( 2.0f,  5.0f, -15.0f),
                new Vector3f(-1.5f, -2.2f, -2.5f),
                new Vector3f(-3.8f, -2.0f, -12.3f),
                new Vector3f( 2.4f, -0.4f, -3.5f),
                new Vector3f(-1.7f,  3.0f, -7.5f),
                new Vector3f( 1.3f, -2.0f, -2.5f),
                new Vector3f( 1.5f,  2.0f, -2.5f),
                new Vector3f( 1.5f,  0.2f, -1.5f),
                new Vector3f(-1.3f,  1.0f, -1.5f)
        };
    
        models = new Model[cubePositions.length];
        Random random = new Random(69420);
        ModelLoader.get().load("cube", () -> Model.ofFile("cube.obj"));

//        List<Node> nodes = ModelLoader.get().get("cube").getNodes();
//        for (int i = 0; i < nodes.size(); i++) {
//            System.out.println(i + " " + nodes.get(i).getMeshes().size());
//        }
//                .getNodes().getFirst()
//                .getMeshes().getFirst()
//                .getMaterial().getTextures().add(
//                        Textures.getSkybox("space")
//                );
        for (int i = 0; i < cubePositions.length; i++) {
            models[i] = ModelLoader.get().get("cube");
            models[i].getTransform()
                    .setPosition(cubePositions[i])
                    .setRotation(new Quaternionf().rotateAxis(
                            random.nextFloat(),
                            new Vector3f(
                                    random.nextFloat(-1f, 1f),
                                    random.nextFloat(-1f, 1f),
                                    random.nextFloat(-1f, 1f)))
                    );
        }
    
        Vector3f[] grassPosition = {
                new Vector3f(-1.5f,  0.0f, -0.48f),
                new Vector3f( 1.5f,  0.0f,  0.51f),
                new Vector3f( 0.0f,  0.0f,  0.7f),
                new Vector3f(-0.3f,  0.0f, -2.3f),
                new Vector3f( 0.5f,  0.0f, -0.6f)
        };
        grassModels = new ArrayList<>(grassPosition.length);
        for (Vector3f position : grassPosition) {
            //TODO contemplate how to make copy on repeated load (so e.g. no individual transform) more obvious
            Model grassModel = ModelLoader.get().load("grass", () ->
                    new Model.Builder("grass.obj")
                            .disableCulling()
                            .hasTransparency()
                            .build());
            grassModel.getTransform().setPosition(position);
            grassModels.add(grassModel);
        }

        lightSources = new Model[2];
        for (int i = 0; i < lightSources.length; i++) {
            lightSources[i] = ModelLoader.get().load("light", () ->
                            new Model.Builder("cube.obj")
                                    .setName("light")
                                    .setTransform(new Transform()
                                            .setScale(0.2f)
                                            .setPosition(new Vector3f(0f, 0f, -5f)))
                                    .build());
        }

        sword = ModelLoader.get().load("sword", () ->
                new Model.Builder("Sting-Sword.obj")
                        .setTransform(new Transform(
                                new Vector3f(0, 0, -2),
                                new Quaternionf().rotateAxis((float) Math.toRadians(90), new Vector3f(1, 0, 0)),
                                new Vector3f(0.1f)))
                        .build()
        );
    
        backpack = ModelLoader.get().load("backpack", () ->
                new Model.Builder("backpack.obj")
                        .setTransform(new Transform(
                                new Vector3f(3, .5f, -2),
                                new Quaternionf().rotateAxis((float) Math.toRadians(-90), new Vector3f(0, 1, 0)),
                                new Vector3f(0.15f)))
                        .build()
        );
    
        //These are essentially intensity factors
        //TODO isn't this kind of stupid? why would a light SOURCE have different kinds of intensities?
        globalLight = new DirectionalLight(new Vector3f(1),
                new Vector3f(.01f), new Vector3f(.1f), new Vector3f(.2f));
    
        lights = new PointLight[lightSources.length];
        for (int i = 0; i < lights.length; i++) {
            lights[i] = new PointLight(lightSources[i].getTransform().getPosition(),
                    new Vector3f(.01f), new Vector3f(.4f), new Vector3f(1),
                    1f, .03f, .005f);
        }
    }
    
    private void loadShaders() {
        containerShader = Shaders.getStandardShader();//getContainerShader();
        lightShader = Shaders.getLightSourceShader();
        swordShader = Shaders.getSwordShader();
        backpackShader = Shaders.getBackpackShader();
        skyboxShader = Shaders.getCubeMapShader();
    }
    
    private void initUI() {
//        Container container = new Container();
//        //container.getLayout().setAlignment(Layout.Alignment.BOTTOM_RIGHT);
//        Layout layout = Layout.get()
//                .setPrefSize(new Vector2f(400f, 100f))
//                .setMinSize(new Vector2f(250f, 50f));
//        Button button1 = new Button(layout);
//        Button button2 = new Button(layout);
//        Button button3 = new Button(layout);
//        VerticalGroup menu = new VerticalGroup(button1, button2, button3);
//        //menu.layout();
//        //menu.getLayout().setAlignment(Layout.Alignment.CENTER);
//        container.setChild(menu);
//
//        window.setScene(new Scene(new Batch(renderer), container, new OrthographicCamera(window.getSize().toVec())));
//        window.getScene().hide();
    }
    
    private void setShaderUniforms() {
        ShaderProgram[] doLighting = {containerShader, swordShader, backpackShader};
        
        for (ShaderProgram shader : doLighting) {
            shader.setUniformArray("globalLights[$]", 0, globalLight);
            shader.setUniformArray("lights[$]", lights);
        }
    
        //TODO consider passing fragment position to frag shader with view applied,
        // so this nonsense becomes unnecessary
        containerShader.setUniform("uViewPosition", camera.getPosition());
        containerShader.setUniform("uTime", (float) pacer.getTime(), false);
    
        swordShader.setUniform("uViewPosition", camera.getPosition());
        swordShader.setUniform("uTime", (float) pacer.getTime(), false);
    
        backpackShader.setUniform("uViewPosition", camera.getPosition());
        
        lightShader.setUniform("light", lights[0]);
    }
    
    private void updateModels() {
        double radius = 4f, speed = 50f, time = speed * pacer.getTime();
        Vector3f offset = new Vector3f(0f, 0f, -2f);
        Vector3f newLightSourcePos = new Vector3f(
                (float) (radius * Math.cos(Math.toRadians(time))),
                0f,
                (float) (radius * Math.sin(Math.toRadians(time)))
        );
        if (!paused) {
            lightSources[0].getTransform().setPosition(new Vector3f(newLightSourcePos).add(offset));
            lightSources[1].getTransform().setPosition(newLightSourcePos.negate().add(offset));
        }
    
        if (!paused) {
            for (Model cube : models)
                cube.getTransform().getRotation().w = (cube.getTransform().getRotation().w() + .01f);
        }
    }
    
    private void renderModels() {
        renderer.prepare();

        Vector3f translation = camera.getCombined().getTranslation(new Vector3f());
        renderer.render(skybox, skyboxShader, camera.getCombined().translate(translation.negate(), new Matrix4f()).scale(50f));

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
        Vector3fc camPos = camera.getPosition();
        grassModels.sort((model1, model2) -> Float.compare(
                new Vector3f(camPos).sub(model2.getTransform().getPosition()).length(),
                new Vector3f(camPos).sub(model1.getTransform().getPosition()).length())
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
        skyboxShader.dispose();
    
        modelLoader.dispose();
    
        glfwTerminate();
        
        glFlush();
        GL.destroy();
    }
    
    public static void main(String[] args) {
        new DemCubez();
    }
    
}

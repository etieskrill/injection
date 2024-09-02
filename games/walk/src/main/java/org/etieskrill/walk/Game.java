package org.etieskrill.walk;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.OverruleGroup;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Button;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.scene.component.Node.Alignment;
import org.etieskrill.engine.scene.component.Stack;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.util.Loaders.ModelLoader;
import org.etieskrill.engine.util.Loaders.ShaderLoader;
import org.etieskrill.engine.window.Window;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11C.*;

public class Game {
    
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    
    private LoopPacer pacer;
    
    private Window window;
    
    private Camera camera, uiCamera;
    private GLRenderer renderer = new GLRenderer();

    private Scene gameScene, pauseScene;
    
    private Label scoreLabel;
    private Label fpsLabel;
    
    private Model skelly, skellyBBModel;
    private final Vector3f skellyBBModelPosition = new Vector3f();
    private Model cube, light;
    private PointLight pointLight;
    private PointLight[] pointLights;
    private Model[] orbs;
    
    private final Vector3f deltaPos = new Vector3f(0);
    private float rotation, smoothRotation;
    private float jumpTime;
    private float smoothSkellyHeight;
    
    private double prevCursorPosX, prevCursorPosY;
    
    private int orbsCollected;

    private final KeyInputManager keyInputManager = Input.of(
            Input.bind(Keys.ESC).to(() -> {
                if (pacer.isPaused()) unpause();
                else pause();
            }),
            Input.bind(Keys.ESC.withMods(Keys.SHIFT)).to(() -> window.close()),
            Input.bind(Keys.W).on(Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.S).to(() -> deltaPos.add(0, 0, 1)),
            Input.bind(Keys.S).on(Trigger.PRESSED).to(() -> deltaPos.add(0, 0, -1)),
            Input.bind(Keys.A).on(Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.D).to(() -> deltaPos.add(-1, 0, 0)),
            Input.bind(Keys.D).on(Trigger.PRESSED).to(() -> deltaPos.add(1, 0, 0))
    );
    
    Shaders.StaticShader shader;
    Shaders.LightSourceShader lightShader;
    Shaders.TextShader fontShader;
    
    public Game() {
        setupWindow();
        setupUI();
        loop();
        exit();
    }
    
    private void setupWindow() {
        window = Window.builder()
                .setRefreshRate(0)
                .setMode(Window.WindowMode.BORDERLESS)
                .setTitle("Walk")
                .setKeyInputHandlers(keyInputManager)
                .build();
        window.getCursor().disable();

        camera = new PerspectiveCamera(window.getSize().getVec());
        camera.setZoom(4.81f);

        setupCursor();
    }
    
    private void setupCursor() {
        window.addCursorInputs(
                new CursorCameraController(camera, .04, 0)
                        .setUpdateCondition(() -> !pacer.isPaused())
        );
    }
    
    private void setupUI() {
        uiCamera = new OrthographicCamera(window.getSize().getVec());
        
        scoreLabel = new Label("", Fonts.getDefault(48));
        scoreLabel.setAlignment(Alignment.TOP).setMargin(new Vector4f(20));
        
        fpsLabel = new Label("", Fonts.getDefault(36));
        fpsLabel.setAlignment(Alignment.TOP_LEFT).setMargin(new Vector4f(10));
        
        Stack stack = new Stack(List.of(scoreLabel, fpsLabel));

        gameScene = new Scene(
                new Batch(renderer).setShader(Shaders.getTextShader()),
                stack,
                uiCamera);
        window.setScene(gameScene);

        Label labelContinue = new Label("Continue", Fonts.getDefault(48));
        labelContinue.setAlignment(Alignment.CENTER).setMargin(new Vector4f(10));
        Button buttonContinue = new Button(labelContinue);
        buttonContinue.setSize(new Vector2f(300, 100));
        buttonContinue.setAlignment(Alignment.CENTER);
        buttonContinue.setAction(this::unpause);

        Container container = new Container();
        container.setChild(buttonContinue);

        pauseScene = new Scene(
                new Batch(renderer),
                container,
                uiCamera
        );
    }
    
    private void loop() {
        //TODO figure out a smart way to link the pacer and window refresh rates
        pacer = new SystemNanoTimePacer(1 / 60f);
        
        ModelLoader models = ModelLoader.get();
        
        cube = models.load("cube", () -> Model.ofFile("cube.obj"));
        cube.getTransform().setScale(50).setPosition(new Vector3f(0, -25, 0));
        light = models.get("cube");
        light.getTransform().setScale(0.5f).setPosition(new Vector3f(2, 5, -2));
        skelly = models.load("skelly", () -> Model.ofFile("skeleton.glb"));
        skelly.getTransform().setScale(15);
        
        skellyBBModel = ModelFactory.box(skelly.getBoundingBox().getMax().sub(skelly.getBoundingBox().getMin(), new Vector3f()));
        skellyBBModel.getInitialTransform().setPosition(
                skellyBBModelPosition.set(skelly.getBoundingBox().getCenter()).mul(skelly.getTransform().getScale())
        );
        
        final int NUM_ORBS = 10;
        orbs = new Model[NUM_ORBS];
        Random random = new Random(69420);
        for (int i = 0; i < NUM_ORBS; i++) {
            orbs[i] = models.load("orb", () -> Model.ofFile("Sphere.obj"));
            orbs[i].getTransform()
                    .setScale(new Vector3f(.02f))
                    .setPosition(new Vector3f(
                            random.nextFloat() * 50 - 25,
                            random.nextFloat() * 4.5f + .5f,
                            random.nextFloat() * 50 - 25
                    ));
        }
        Model orbBBModel = ModelFactory.box(orbs[0].getBoundingBox().getMax().sub(orbs[0].getBoundingBox().getMin(), new Vector3f()));
        orbBBModel.getInitialTransform().setPosition(
                orbBBModel.getBoundingBox().getCenter()
        );
        
        shader = (Shaders.StaticShader) ShaderLoader.get().load("standard", Shaders::getStandardShader);
        lightShader = (Shaders.LightSourceShader) ShaderLoader.get().load("light", Shaders::getLightSourceShader);
        fontShader = (Shaders.TextShader) ShaderLoader.get().load("font", Shaders::getTextShader);

        pointLight = new PointLight(light.getTransform().getPosition(),
                new Vector3f(1), new Vector3f(1), new Vector3f(1),
                1f, 0.03f, 0.005f);
        pointLights = new PointLight[]{pointLight};
        Font font = null;
        try {
            TrueTypeFont generatorFont = new TrueTypeFont("AGENCYB.TTF");
            font = generatorFont.generateBitmapFont(48);
        } catch (IOException e) {
            logger.warn("Failed to load font");
        }

        //TODO this here section causes some rather flaky behaviour in relation to the size of the above generated
        // bitmap, sometimes when shrinking the font to a size < 14 it will randomly cause a STATUS_HEAP_CORRUPTION,
        // and i haven't a single clue as to why :>
//        if (font != null) {
//            Glyph glyphE = font.getGlyph('E');
//            Texture2D texE = glyphE.getTexture();
//            ByteBuffer bufferE = BufferUtils.createByteBuffer(texE.getPixelSize().getX() * texE.getPixelSize().getY());
//            glReadPixels(0, 0, texE.getPixelSize().getX(), texE.getPixelSize().getY(), GL_RED, GL_UNSIGNED_BYTE, bufferE);
//            StringBuilder builder = new StringBuilder();
//            for (int i = 0; i < bufferE.remaining(); i++) {
//                //bufferE.get() > 0 ? "x " : "  "
//                builder.append(bufferE.get()).append(" ");
//                if ((i + 1) % glyphE.getSize().getX() == 0) builder.append("\n");
//            }
//            System.out.println(builder);
//        }
    
//        TODO funny how these return completely inaccurate results
//         test isolated with kotlin, java, and play back to https://github.com/kotlin-graphics/glm if necessary
//        System.out.println(new Vector2i(1000).times(0.5f));
//        System.out.println(new Vector2i(1000));
//        System.out.println(new Vector2i(1000).times(1.5f));
//        System.out.println(new Vector2i(1000).times(2f));

        FrameBuffer postBuffer = FrameBuffer.getStandard(window.getSize().getVec());
        Material mat = new Material.Builder() //TODO okay, the fact models, or rather meshes simply ignore these mats is getting frustrating now, that builder needs some serious rework
                .addTextures((Texture2D) postBuffer.getAttachment(BufferAttachmentType.COLOUR0))
                .build();
        Model screenQuad = ModelFactory.rectangle(-1, -1, 2, 2, mat) //Hey hey people, these, are in fact, regular    screeeeen coordinates, not viewport, meaning, for post processing, these effectively *always* need to be    (-1, -1) and (2, 2).
                .build();
        screenQuad.getTransform().setPosition(new Vector3f(0, 0, 1));
        Shaders.PostprocessingShader screenShader = Shaders.getPostprocessingShader();
        Vector3f unpauseColour = new Vector3f(1), pauseColour = new Vector3f(.65f);
        
        pacer.start();
        while (!window.shouldClose()) {
            if (!pacer.isPaused()) {
                transformSkelly();
                collideOrbs();
                updateCamera();
            }

            //TODO add start menu
            //     add victory screen / with score
            //     add animation, fixed / skeletal
            //     create component system
            //     pack common components
            //     create "world" for updateable objects    mfw when i learned about entity systems: *surprised pikachu*
            //     abstract all gl/ram resources from objects and share accordingly
            //     add options menu
            //      - screen options
            //      - sens options
            //      - keybinds

            //TODO meta
            //     editorconfig
    
            postBuffer.bind();
            renderer.prepare();
            render();
            postBuffer.unbind();
            
            renderer.prepare();
            screenShader.setUniform("emboss", true);
            screenShader.setUniform("colour", pacer.isPaused() ? pauseColour : unpauseColour);
            renderer.render(screenQuad.getTransform(), screenQuad, screenShader, (Camera) null);
            
            glDisable(GL_DEPTH_TEST);
            
            scoreLabel.setText("Orbes collectered: " + orbsCollected);
            fpsLabel.setText("%5.3f\n%7.6f".formatted(pacer.getAverageFPS(), pacer.getDeltaTimeSeconds()));
            
            glEnable(GL_DEPTH_TEST);
            
            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }
        
        if (font != null) font.dispose();
    }

    private void unpause() {
        pacer.resumeTimer();
        window.getCursor().disable();
        window.setScene(gameScene);
    }

    private void pause() {
        pacer.pauseTimer();
        window.getCursor().capture();
        window.setScene(pauseScene);
    }
    
    private void transformSkelly() {
        Vector3f skellyTranslate = camera
                .relativeTranslation(deltaPos)
                .mul(1, 0, 1);
        if (skellyTranslate.lengthSquared() > 0) skellyTranslate.normalize();
        skellyTranslate
                .mul((float) pacer.getDeltaTimeSeconds())
                .mul(5f * (keyInputManager.isPressed(Keys.CTRL) ? 1.5f : 1f));
        skelly.getTransform().translate(skellyTranslate);

        if (!deltaPos.equals(0, 0, 0)) {
            rotation = (float) (Math.atan2(deltaPos.z(), deltaPos.x()) - Math.toRadians(camera.getYaw()));
            rotation %= Math.toRadians(360);
            //TODO fix shortest distance through wraparound
            //TODO include delta time
            if (Math.abs(rotation - smoothRotation) > 0.001) {
                double falloff = -0.5 * (1 / (2 * Math.abs(rotation - smoothRotation) + 0.5)) + 1;
                falloff *= 500 * pacer.getDeltaTimeSeconds();
                smoothRotation += Math.toRadians(rotation - smoothRotation >= 0 ? falloff : -falloff);
            }
            smoothRotation %= Math.toRadians(360);
            skelly.getTransform().getRotation().rotationY(smoothRotation);
        }
        deltaPos.set(0);
    
        skelly.getTransform().getPosition().x = Math.max(-25, Math.min(25, skelly.getTransform().getPosition().x())); //why not use Math#clamp? try it, smartass
        skelly.getTransform().getPosition().z = Math.max(-25, Math.min(25, skelly.getTransform().getPosition().z()));

        if (keyInputManager.isPressed(Keys.SPACE) && jumpTime == 0)
            jumpTime += 0.0001;
        if (jumpTime != 0 && jumpTime < 1) {
            double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
            skelly.getTransform().getPosition().y = (float) jumpHeight;
            jumpTime += pacer.getDeltaTimeSeconds();
        } else jumpTime = 0;
    
        float skellyHeight;
        if (keyInputManager.isPressed(Keys.LEFT_SHIFT)) skellyHeight = 9;
        else skellyHeight = 15;
    
        double falloff = -0.5 * (1 / (2 * Math.abs(skellyHeight - smoothSkellyHeight) + 0.5)) + 1;
        falloff = 20 * falloff * pacer.getDeltaTimeSeconds();
        smoothSkellyHeight += skellyHeight > smoothSkellyHeight ? falloff : -falloff;
    
        skelly.getTransform().setScale(new Vector3f(15, smoothSkellyHeight, 15));

//            skellyBBModel.getTransform().set(skelly.getTransform());
    }
    
    private void collideOrbs() {
        for (Model orb : orbs) {
            if (!orb.isEnabled()) continue;
            
            Vector3f direction = orb.getTransform().getPosition().sub(skelly.getTransform().getPosition(), new Vector3f());
            if (new Vector2f(direction.x(), direction.z()).length() < 1 && direction.y() > 0 && direction.y() < 4) {
                orb.disable();
                orbsCollected++;
            }
        }
    }
    
    private void updateCamera() {
        Vector3f orbitPos = camera.getDirection().negate().mul(3);
        float skellyWorldSpaceHeight = skelly.getTransform().getScale().y() * skelly.getBoundingBox().getSize().y() - .5f;
        camera.setPosition(orbitPos.add(0, skellyWorldSpaceHeight, 0).add(skelly.getTransform().getPosition()));
    }
    
    private void render() {
        shader.setViewPosition(camera.getPosition());
        shader.setLights(pointLights);

        renderer.render(cube.getTransform(), cube, shader, camera);
        renderer.render(skelly.getTransform(), skelly, shader, camera);
//            renderer.renderWireframe(skellyBBModel, shader, camera.getCombined());
    
        for (Model orb : orbs) {
            if (!orb.isEnabled()) continue;
            renderer.render(orb.getTransform(), orb, shader, camera);
//                orbBBModel.getTransform().set(orb.getTransform());
//                renderer.renderWireframe(orbBBModel, shader, camera.getCombined());
        }
    
        lightShader.setLight(pointLight);
        renderer.render(light.getTransform(), light, lightShader, camera);
    }
    
    private void exit() {
        window.dispose();
        Loaders.disposeDefaultLoaders();
        TrueTypeFont.disposeLibrary();
        System.exit(0);
    }
    
    public static void main(String[] args) {
        new Game();
    }
    
}

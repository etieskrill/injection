package org.etieskrill.walk;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.OrthographicCamera;
import org.etieskrill.engine.graphics.PerspectiveCamera;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.FrameBuffer;
import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.PointLight;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.InputManager;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.OverruleGroup;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.util.Loaders.ModelLoader;
import org.etieskrill.engine.util.Loaders.ShaderLoader;
import org.etieskrill.engine.window.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;

public class Game {
    
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    
    private LoopPacer pacer;
    
    private Window window;
    
    private Camera camera;
    
    private Model skelly, skellyBBModel;
    private Model[] orbs;
    
    private final Vec3 deltaPos = new Vec3(0);
    private float rotation, smoothRotation;
    private float jumpTime;
    private float smoothSkellyHeight;
    
    private double prevCursorPosX, prevCursorPosY;
    
    private int orbsCollected;
    
    private final InputManager inputManager = Input.of(
            Input.bind(Keys.ESC).to(() -> {
                if (pacer.isPaused()) {
                    pacer.resumeTimer();
                    window.getCursor().disable();
                }
                else {
                    pacer.pauseTimer();
                    window.getCursor().enable();
                }
            }),
            Input.bind(Keys.ESC.withMods(Keys.SHIFT)).to(() -> window.close()),
            Input.bind(Keys.W).on(Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.S).to(() -> deltaPos.plusAssign(0, 0, 1)),
            Input.bind(Keys.S).on(Trigger.PRESSED).to(() -> deltaPos.plusAssign(0, 0, -1)),
            Input.bind(Keys.A).on(Trigger.PRESSED).group(OverruleGroup.Mode.YOUNGEST, Keys.D).to(() -> deltaPos.plusAssign(-1, 0, 0)),
            Input.bind(Keys.D).on(Trigger.PRESSED).to(() -> deltaPos.plusAssign(1, 0, 0))
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
            if (!pacer.isPaused()) camera.orient(-dy * sens, dx * sens, 0);
    
            prevCursorPosX = xpos;
            prevCursorPosY = ypos;
        }));
    }
    
    private void loop() {
        //TODO figure out a smart way to link the pacer and window refresh rates
        pacer = new SystemNanoTimePacer(1 / 60f);
        
        Model cube = ModelLoader.get().load("cube", () -> Model.ofFile("cube.obj"));
        cube.getTransform().setScale(50).setPosition(new Vec3(0, -25, 0));
        Model light = ModelLoader.get().get("cube");
        light.getTransform().setScale(0.5f).setPosition(new Vec3(2, 5, -2));
        skelly = ModelLoader.get().load("skelly", () -> Model.ofFile("skeleton.glb"));
        skelly.getTransform().setScale(15);
        
        skellyBBModel = ModelFactory.box(skelly.getBoundingBox().getMax().minus(skelly.getBoundingBox().getMin()));
        skellyBBModel.getTransform()
                .setInitialPosition(
                        skelly.getBoundingBox().getCenter().times(skelly.getTransform().getScale())
                );
        
        final int NUM_ORBS = 10;
        orbs = new Model[NUM_ORBS];
        Random random = new Random(69420);
        for (int i = 0; i < NUM_ORBS; i++) {
            orbs[i] = ModelLoader.get().load("orb", () -> Model.ofFile("Sphere.obj"));
            orbs[i].getTransform()
                    .setScale(new Vec3(0.02))
                    .setPosition(new Vec3(
                            random.nextFloat() * 50 - 25,
                            random.nextFloat() * 4.5 + 0.5,
                            random.nextFloat() * 50 - 25
                    ));
        }
        Model orbBBModel = ModelFactory.box(orbs[0].getBoundingBox().getMax().minus(orbs[0].getBoundingBox().getMin()));
        orbBBModel.getTransform().setInitialPosition(
                orbBBModel.getBoundingBox().getCenter()
        );
        
        Renderer renderer = new Renderer();
        
        Shaders.StaticShader shader = (Shaders.StaticShader) ShaderLoader.get().load("standard", Shaders::getStandardShader);
        Shaders.LightSourceShader lightShader = (Shaders.LightSourceShader) ShaderLoader.get().load("light", Shaders::getLightSourceShader);
        Shaders.TextShader fontShader = (Shaders.TextShader) ShaderLoader.get().load("font", Shaders::getTextShader);
        
        camera = new PerspectiveCamera(window.getSize().toVec());
        camera.setZoom(4.81f);

        PointLight pointLight = new PointLight(light.getTransform().getPosition(),
                new Vec3(1), new Vec3(1), new Vec3(1),
                1f, 0.03f, 0.005f);
        PointLight[] pointLights = {pointLight};
        Font font = null;
        try {
            TrueTypeFont generatorFont = new TrueTypeFont("agencyb.ttf");
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
        
        Vec2 windowSize = window.getSize().toVec();
        Camera uiCamera = new OrthographicCamera(windowSize)
                .setOrientation(0, -90, 0)
                .setPosition(new Vec3(windowSize.times(0.5), 0))
        ;
    
//        TODO funny how these return completely inaccurate results
//         test isolated with kotlin, java, and play back to https://github.com/kotlin-graphics/glm if necessary
//        System.out.println(new Vec2i(1000).times(0.5f));
//        System.out.println(new Vec2i(1000));
//        System.out.println(new Vec2i(1000).times(1.5f));
//        System.out.println(new Vec2i(1000).times(2f));
        
        FrameBuffer postBuffer = FrameBuffer.getStandard(new Vec2i(windowSize));
        Material mat = new Material.Builder() //TODO okay, the fact models, or rather meshes simply ignore these mats is getting frustrating now, that builder needs some serious rework
                .addTextures((Texture2D) postBuffer.getAttachment(FrameBuffer.AttachmentType.COLOUR0))
                .build();
        Model screenQuad = ModelFactory.rectangle(-1, -1, 2, 2, mat) //Hey hey people, these, are in fact, regular    screeeeen coordinates, not viewport, meaning, for post processing, these effectively *always* need to be    (-1, -1) and (2, 2).
                .build();
        Shaders.ScreenQuadShader screenShader = Shaders.getScreenShader();
        
        pacer.start();
        while (!window.shouldClose()) {
            if (!pacer.isPaused()) {
                transformSkelly();
                collideOrbs();
                updateCamera();
            }

            //TODO add pause menu
            //     add start menu
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
            
            if (font != null) {
                renderer.render(
                        font.getGlyphs("Orbes collectered: " + orbsCollected),
                        new Vec2(0),
                        fontShader,
                        uiCamera.getCombined()
                );
            }
            
            shader.setViewPosition(camera.getPosition());
            shader.setLights(pointLights);
            
            renderer.render(cube, shader, camera.getCombined());
            renderer.render(skelly, shader, camera.getCombined());
//            renderer.renderWireframe(skellyBBModel, shader, camera.getCombined());
            
            for (Model orb : orbs) {
                if (!orb.isEnabled()) continue;
                renderer.render(orb, shader, camera.getCombined());
//                orbBBModel.getTransform().set(orb.getTransform());
//                renderer.renderWireframe(orbBBModel, shader, camera.getCombined());
            }
            
            lightShader.setLight(pointLight);
            renderer.render(light, lightShader, camera.getCombined());
            
            postBuffer.unbind();
            
            renderer.prepare();
            renderer.render(screenQuad, screenShader, null);
            
            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }
        
        if (font != null) font.dispose();
    }
    
    private void transformSkelly() {
        Vec3 skellyTranslate = camera
                .relativeTranslation(deltaPos)
                .times(1, 0, 1);
        if (skellyTranslate.length2() > 0) skellyTranslate.normalizeAssign();
        skellyTranslate = skellyTranslate
                .times(pacer.getDeltaTimeSeconds())
                .times(5 * (inputManager.isPressed(Keys.CTRL) ? 1.5 : 1));
        skelly.getTransform().translate(skellyTranslate);
    
        if (deltaPos.anyNotEqual(0, 0.001f)) {
            rotation = (float) (Math.atan2(deltaPos.getZ(), deltaPos.getX()) - Math.toRadians(camera.getYaw()));
            rotation %= Math.toRadians(360);
            //TODO fix shortest distance through wraparound
            //TODO include delta time
            if (Math.abs(rotation - smoothRotation) > 0.001) {
                double falloff = -0.5 * (1 / (2 * Math.abs(rotation - smoothRotation) + 0.5)) + 1;
                smoothRotation += Math.toRadians(rotation - smoothRotation >= 0 ? 5 * falloff : -5 * falloff);
            }
            smoothRotation %= Math.toRadians(360);
            skelly.getTransform().setRotation(
                    smoothRotation,
                    new Vec3(0, 1, 0));
        }
        deltaPos.put(0);
    
        skelly.getTransform().getPosition().setX(Math.max(-25, Math.min(25, skelly.getTransform().getPosition().getX()))); //why not use Math#clamp? try it, smartass
        skelly.getTransform().getPosition().setZ(Math.max(-25, Math.min(25, skelly.getTransform().getPosition().getZ())));
    
        if (inputManager.isPressed(Keys.SPACE) && jumpTime == 0)
            jumpTime += 0.0001;
        if (jumpTime != 0 && jumpTime < 1) {
            double jumpHeight = -4 * (jumpTime - 0.5) * (jumpTime - 0.5) + 1;
            skelly.getTransform().getPosition().setY((float) jumpHeight);
            jumpTime += pacer.getDeltaTimeSeconds();
        } else jumpTime = 0;
    
        float skellyHeight;
        if (inputManager.isPressed(Keys.LEFT_SHIFT)) skellyHeight = 9;
        else skellyHeight = 15;
    
        double falloff = -0.5 * (1 / (2 * Math.abs(skellyHeight - smoothSkellyHeight) + 0.5)) + 1;
        falloff = 20 * falloff * pacer.getDeltaTimeSeconds();
        smoothSkellyHeight += skellyHeight > smoothSkellyHeight ? falloff : -falloff;
    
        skelly.getTransform().setScale(new Vec3(15, smoothSkellyHeight, 15));

//            skellyBBModel.getTransform().set(skelly.getTransform());
    }
    
    private void collideOrbs() {
        for (Model orb : orbs) {
            if (!orb.isEnabled()) continue;
            
            Vec3 direction = orb.getTransform().getPosition().minus(skelly.getTransform().getPosition());
            if (new Vec2(direction.getX(), direction.getZ()).length() < 1 && direction.getY() > 0 && direction.getY() < 4) {
                orb.disable();
                orbsCollected++;
            }
        }
    }
    
    private void updateCamera() {
        Vec3 orbitPos = camera.getDirection().negate().times(3);
        camera.setPosition(orbitPos.plus(0, skelly.getWorldBoundingBox().getSize().getY() - 0.5, 0).plus(skelly.getTransform().getPosition()));
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

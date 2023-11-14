package org.etieskrill.orbes;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.OrthographicCamera;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.FrameBuffer;
import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.etieskrill.orbes.scene.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.orbes.Game.Stage.*;

public class Game {

    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private LoopPacer pacer;

    private Window window;

    private Renderer renderer = new Renderer();

    private GameScene gameScene;
    private MainMenuUIScene mainMenuScene;
    private GameUIScene gameUIScene;
    private GameUIPauseScene pauseUIScene;
    private EndScene endScene;

    private Stage stage;

    public enum Stage {
        MAIN_MENU,
        OPTIONS,
        GAME,
        END
    }

    public Game() {
        setupWindow();
        setupUI();
        this.gameScene = new GameScene(this);

        loop();
        exit();
    }

    public void unpause() {
        if (stage != GAME) return;

        pacer.resumeTimer();
        window.getCursor().disable();
        window.setScene(gameUIScene);
    }

    public void pause() {
        if (stage != GAME) return;

        pacer.pauseTimer();
        window.getCursor().capture();
        window.setScene(pauseUIScene);
    }

    public Stage getStage() {
        return stage;
    }

    public Window getWindow() {
        return window;
    }

    public LoopPacer getPacer() {
        return pacer;
    }

    private void setupWindow() {
        window = new Window.Builder()
                .setRefreshRate(0)
                .setMode(Window.WindowMode.BORDERLESS)
                .setTitle("Walk")
                .build();
    }

    private void setupUI() {
        Vec2 windowSize = window.getSize().toVec();

        mainMenuScene = new MainMenuUIScene(
                new Batch(renderer),
                new OrthographicCamera(windowSize).setPosition(new Vec3(windowSize.times(0.5))),
                this
        );

        gameUIScene = new GameUIScene(
                new Batch(renderer).setShader(Shaders.getTextShader()),
                new OrthographicCamera(windowSize),
                windowSize);

        pauseUIScene = new GameUIPauseScene(
                new Batch(renderer),
                new OrthographicCamera(windowSize).setPosition(new Vec3(windowSize.times(0.5))),
                this
        );

        endScene = new EndScene(
                new Batch(renderer),
                new OrthographicCamera(windowSize).setPosition(new Vec3(windowSize.times(0.5))),
                this
        );
    }

    private void loop() {
        //TODO figure out a smart way to link the pacer and window refresh rates
        pacer = new SystemNanoTimePacer(1 / 60f);

        Vec2 windowSize = window.getSize().toVec();
        FrameBuffer postBuffer = FrameBuffer.getStandard(new Vec2i(windowSize));
        Material mat = new Material.Builder() //TODO okay, the fact models, or rather meshes simply ignore these mats is getting frustrating now, that builder needs some serious rework
                .addTextures((Texture2D) postBuffer.getAttachment(FrameBuffer.AttachmentType.COLOUR0))
                .build();
        Model screenQuad = ModelFactory.rectangle(-1, -1, 2, 2, mat) //Hey hey people, these, are in fact, regular    screeeeen coordinates, not viewport, meaning, for post processing, these effectively *always* need to be    (-1, -1) and (2, 2).
                .build();
        screenQuad.getTransform().setPosition(new Vec3(0, 0, 1));
        Shaders.PostprocessingShader screenShader = Shaders.getPostprocessingShader();
        Vec3 unpauseColour = new Vec3(1.0), pauseColour = new Vec3(0.65);

        showMainMenu();

        pacer.start();
        while (!window.shouldClose()) {
            update(pacer.getDeltaTimeSeconds());

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
            gameScene.render(renderer);
            postBuffer.unbind();

            renderer.prepare();
            screenShader.setUniform("uEmboss", true);
            screenShader.setUniform("uColour", pacer.isPaused() ? pauseColour : unpauseColour);
            renderer.render(screenQuad, screenShader, null);

            if (stage == GAME) {
                gameUIScene.getScoreLabel().setText("Orbes collectered: " + gameScene.getOrbsCollected());
                gameUIScene.getFpsLabel().setText("%5.3f\n%7.6f".formatted(pacer.getAverageFPS(), pacer.getDeltaTimeSeconds()));

                if (gameScene.getOrbsCollected() == GameScene.NUM_ORBS) {
                    showEndScreen(EndScene.Status.VICTORY);
                }
            }

            window.update(pacer.getDeltaTimeSeconds());
            pacer.nextFrame();
        }
    }

    private void update(double delta) {
        switch (stage) {
            case MAIN_MENU, END -> {
                Camera camera = gameScene.getCamera();
                double time = pacer.getTime() * 0.25;

                Vec3 pos = new Vec3(Math.cos(time), 0.5, Math.sin(time)).times(15);
                camera.setPosition(pos);
                camera.setOrientation(-20, Math.toDegrees(Math.atan2(-pos.getZ(), -pos.getX())), 0); //TODO camera utils for orbiting / lookat (literally is the implementation already)
            }
            case GAME -> {
                if (!pacer.isPaused()) {
                    gameScene.updateScene(pacer.getDeltaTimeSeconds());
                    gameScene.updateCamera();
                }
            }
        }
    }

    public void showMainMenu() {
        gameScene.reset();
        window.setInputs(null);
        window.setScene(mainMenuScene);
        this.stage = MAIN_MENU;
        pacer.resumeTimer();
    }

    public void showGame() {
        window.setInputs(gameScene.getKeyInputManager());
        this.stage = GAME;
        unpause();
    }

    public void showEndScreen(EndScene.Status status) {
        window.setInputs(null);
        window.setScene(endScene.setStatus(status));
        window.getCursor().capture();
        this.stage = END;
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

package org.etieskrill.orbes;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.service.impl.BoundingBoxRenderService;
import org.etieskrill.engine.entity.service.impl.BoundingBoxService;
import org.etieskrill.engine.entity.service.impl.RenderService;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.font.TrueTypeFont;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.engine.time.SystemNanoTimePacer;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.etieskrill.orbes.scene.*;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;

import static org.etieskrill.orbes.Game.Stage.*;

public class Game extends GameApplication {

    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private static final double MAX_TIME = 40.0;

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
        super(Window.builder()
                .setRefreshRate(0)
                .setMode(Window.WindowMode.BORDERLESS)
                .setTitle("Walk")
                .build());
    }

    @Override
    protected void init() {
        GLUtils.addDebugLogging();
        setupUI();
        this.gameScene = new GameScene(this);

//        Material mat = new Material.Builder() //TODO okay, the fact models, or rather meshes simply ignore these mats is getting frustrating now, that builder needs some serious rework
//                .addTextures((Texture2D) postBuffer.getAttachment(BufferAttachmentType.COLOUR0))
//                .build();
        Vector3f unpauseColour = new Vector3f(1.0f), pauseColour = new Vector3f(0.65f);

        showMainMenu();

        entitySystem.addServices(
                new BoundingBoxService(),
                new RenderService(renderer, gameScene.getCamera(), window.getSize().getVec()),
                new BoundingBoxRenderService(renderer, gameScene.getCamera())
        );
    }

    private void setupUI() {
        Vector2ic windowSize = window.getSize().getVec();

        mainMenuScene = new MainMenuUIScene(
                new Batch(renderer),
                new OrthographicCamera(new Vector2i(windowSize)),
                this
        );

        gameUIScene = new GameUIScene(
                new Batch(renderer),
                new OrthographicCamera(new Vector2i(windowSize)),
                new Vector2f(windowSize));

        pauseUIScene = new GameUIPauseScene(
                new Batch(renderer),
                new OrthographicCamera(new Vector2i(windowSize)),
                this
        );

        endScene = new EndScene(
                new Batch(renderer),
                new OrthographicCamera(new Vector2i(windowSize)),
                this
        );
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

    @Override
    protected void loop(double delta) {
        update(pacer.getDeltaTimeSeconds());

        //TODO add animation, fixed / skeletal
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

//        renderer.prepare();
//        screenShader.doEmboss(false);
//        screenShader.setColour(pacer.isPaused() ? pauseColour : unpauseColour);
//        screenShader.doGammaCorrection(true);

//        renderer.render(screenQuad.getTransform(), screenQuad, screenShader, new Matrix4f());

        if (stage == GAME) {
            gameUIScene.getScoreLabel().setText("Orbes collectered: " + gameScene.getOrbsCollected());
            gameUIScene.getFpsLabel().setText("%5.3f\n%7.6f".formatted(pacer.getAverageFPS(), pacer.getDeltaTimeSeconds()));

            if (pacer.getTime() > MAX_TIME) {
                showEndScreen(EndScene.Status.TIMEOUT, gameScene.getOrbsCollected());
            } else if (gameScene.getOrbsCollected() == GameScene.NUM_ORBS) {
                showEndScreen(EndScene.Status.VICTORY, gameScene.getOrbsCollected());
            }
        }
    }

    @VisibleForTesting
    void update(double delta) {
        switch (stage) {
            case MAIN_MENU, END -> {
                Camera camera = gameScene.getCamera();
                double time = pacer.getSecondsElapsedTotal() * 0.25;

                Vector3f pos = new Vector3f((float) Math.cos(time), 0.5f, (float) Math.sin(time)).mul(15);
                camera.setPosition(pos);
                camera.setRotation(-20, (float) Math.toDegrees(Math.atan2(-pos.z(), -pos.x())), 0); //TODO camera utils for orbiting / lookat (literally is the implementation already)
            }
            case GAME -> {
                if (!pacer.isPaused()) {
                    gameScene.updateScene(pacer.getDeltaTimeSeconds());
                    gameScene.updateCamera();
                    gameUIScene.getTimerLabel().setText("%.1f".formatted(MAX_TIME - pacer.getTime()));
                }
            }
        }
    }

    public void showMainMenu() {
        gameScene.reset();
        window.clearKeyInputs();
        window.setScene(mainMenuScene);
        stage = MAIN_MENU;
    }

    public void showGame() {
        window.setKeyInputs(gameScene.getKeyInputManager());
        pacer.resetTimer();
        stage = GAME;
        unpause();
    }

    public void showEndScreen(EndScene.Status status, int score) {
        window.clearKeyInputs();
        window.setScene(endScene.setStatus(status).setScore(score));
        window.getCursor().capture();
        stage = END;
    }

    public static void main(String[] args) {
        new Game();
    }

}

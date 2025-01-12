package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.impl.SnippetsService;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.impl.GridShader;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.InputBinding;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.KeyCharacterController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.etieskrill.game.horde.component.BillBoard;
import org.etieskrill.game.horde.component.Collider;
import org.etieskrill.game.horde.component.EffectContainer;
import org.etieskrill.game.horde.component.SlowEffect;
import org.etieskrill.game.horde.entity.Enemy;
import org.etieskrill.game.horde.entity.Player;
import org.etieskrill.game.horde.service.BillBoardRenderService;
import org.etieskrill.game.horde.service.DirectionalBillBoardShadowMappingService;
import org.etieskrill.game.horde.service.EffectService;
import org.etieskrill.game.horde.service.SimpleCollisionService;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

import static org.joml.Math.toRadians;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public class Application extends GameApplication {

    Camera camera;
    ShaderProgram gridShader;
    int dummyVao;

    Texture2D floorTexture;
    ShaderProgram floorShader;

    Player dude;
    KeyCharacterController<Vector3f> playerController;

    ShaderProgram stoopidShader;
    DirectionalLightComponent dirLight;

    float daylightCycleRotation = 0;

    public Application() {
        super(Window.builder()
                .setTitle("Horde")
                .setMode(Window.WindowMode.BORDERLESS)
                .setVSyncEnabled(true)
                .setSamples(4)
                .build());
    }

    @Override
    protected void init() {
        GLUtils.addDebugLogging();

        camera = new PerspectiveCamera(window.getSize().getVec())
                .setOrbit(true)
                .setOrbitDistance(2)
                .setZoom(7);

        entitySystem.addService(new EffectService());
        entitySystem.addService(new SimpleCollisionService());
        entitySystem.addService(new SimpleCollisionService());
        entitySystem.addService(new DirectionalBillBoardShadowMappingService(camera));
        entitySystem.addService(new BillBoardRenderService(camera));
        entitySystem.addService(new SnippetsService());

        window.getCursor().disable();

        gridShader = new GridShader();

        dummyVao = glGenVertexArrays();

        floorTexture = new Texture2D.FileBuilder("grass.png")
                .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST).build();
        floorShader = new ShaderProgram(List.of("Floor.glsl"), false) {
        };

        dude = entitySystem.createEntity(id -> new Player(id, pacer)); //FIXME why tf does the player's billboard not render if this is put before the service definitions

        playerController = new KeyCharacterController<>(dude.getTransform().getPosition(), (delta, target, deltaPosition, speed) -> {
            deltaPosition.y = 0;
            if (!deltaPosition.equals(0, 0, 0)) {
                dude.setWalking(true);
                deltaPosition.normalize();
            } else {
                dude.setWalking(false);
            }

            deltaPosition.x = -deltaPosition.x;
            deltaPosition.rotateY(toRadians(camera.getYaw()));
            target.add(deltaPosition.mul((float) delta * speed));
        });
        window.addKeyInputs(playerController);

        window.addKeyInputs(Input.of(
                Input.bind(Keys.A).to(() -> dude.setLookingRight(false)),
                Input.bind(Keys.D).to(() -> dude.setLookingRight(true))
        ));

        entitySystem.createEntity(id -> new Enemy(id, dude.getTransform().getPosition(), camera));

        Random random = new Random(69420);

        var bushTexture = getPixelTexture("bush1.png");
        for (int i = 0; i < 100; i++) {
            entitySystem.createEntity()
                    .withComponent(new Transform().setPosition(new Vector3f(random.nextFloat(-10, 10), 0, random.nextFloat(-10, 10))))
                    .withComponent(new BillBoard(
                            bushTexture,
                            new Vector2f(0.5f)))
                    .withComponent(new Collider(0.25f, true, false, (entity, otherEntity) -> {
                        var effectContainer = otherEntity.getComponent(EffectContainer.class);
                        if (effectContainer != null)
                            effectContainer.add(new SlowEffect(1.5f, 1, "bush"), otherEntity);
                    }));
        }

        var treeTexture = getPixelTexture("tree01.png");
        for (int i = 0; i < 50; i++) {
            entitySystem.createEntity()
                    .withComponent(new Transform().setPosition(new Vector3f(random.nextFloat(-10, 10), 0, random.nextFloat(-10, 10))))
                    .withComponent(new BillBoard(
                            treeTexture,
                            new Vector2f(2, 4),
                            true))
                    .withComponent(new Collider(.5f, true, true));
        }

        dirLight = entitySystem.createEntity()
                .addComponent(new DirectionalLightComponent(
                        new DirectionalLight(new Vector3f(1, -1, 1)),
                        DirectionalShadowMap.generate(new Vector2i(2048)),
                        new OrthographicCamera(new Vector2i(2048),
                                -15, 15, -15, 15)
                                .setRotation(45, 150, 0)
                                .setFar(40)
                                .setNear(-10)
                                .setOrbit(true)
                                .setOrbitDistance(10)
                ));

        stoopidShader = new ShaderProgram(List.of("Blit.glsl")) {
        };

        window.addKeyInputs(Input.of(
                Input.bind(Keys.Q).on(InputBinding.Trigger.TOGGLED).to(delta -> daylightCycleRotation += 20 * delta)
        ));

        GLUtils.removeDebugLogging();
    }

    static Texture2D getPixelTexture(String file) {
        return (Texture2D) Loaders.TextureLoader.get().load(file, () ->
                new Texture2D.FileBuilder(file)
                        .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST)
                        .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_EDGE)
                        .build());
    }

    @Override
    protected void loop(double delta) {
        playerController.setSpeed(dude.getMovementSpeed().getSpeed());

        camera.setRotation(-45, 0, 0);
        camera.setPosition(new Vector3f(dude.getTransform().getPosition()).add(0, 0.5f, 0));

        dirLight.getCamera().setPosition(camera.getPosition());
        dirLight.getCamera().setRotation(45, daylightCycleRotation, 0);

        renderFloor(); //not in #render because floor needs to be drawn first
//        renderGrid();
    }

    @Override
    protected void render() {
        stoopidShader.start();
        stoopidShader.setTexture("tex", dirLight.getShadowMap().getTexture());

        glBindVertexArray(dummyVao);

        glDisable(GL_DEPTH_TEST);
        glDrawArrays(GL_POINTS, 0, 1);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderFloor() {
        glBindVertexArray(dummyVao);

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        floorShader.start();
        floorShader.setUniform("camera", camera);
        floorShader.setTexture("diffuse", floorTexture);
        floorShader.setUniformNonStrict("dirLight", dirLight.getDirectionalLight());
        floorShader.setTexture("dirShadowMap", dirLight.getShadowMap().getTexture());
        floorShader.setUniform("dirLightCombined", dirLight.getCamera().getCombined());

        glDrawArrays(GL_POINTS, 0, 1);
    }

    private void renderGrid() {
        gridShader.start();
        gridShader.setUniform("camera", camera);

        glBindVertexArray(dummyVao);

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDrawArrays(GL_POINTS, 0, 1);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    public static void main(String[] args) {
        new Application();
    }

}
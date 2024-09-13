package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
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
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.KeyCharacterController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.window.Window;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46C;

import java.util.Random;

import static org.joml.Math.sin;
import static org.joml.Math.toRadians;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public class Application extends GameApplication {

    Camera camera;
    ShaderProgram gridShader;
    int dummyVao;

    Texture2D floorTexture;
    ShaderProgram floorShader;

    Transform dudeTransform;
    BillBoard dudeBillBoard;
    boolean dudeWalking = false;
    boolean dudeLooksRight = true;

    ShaderProgram stoopidShader;
    DirectionalLightComponent dirLight;

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
        GLUtils.clearError();
        glNamedStringARB(GL_SHADER_INCLUDE_ARB, "/Camera.glsl", """
                    struct Camera {
                    mat4 perspective;
                    mat4 combined;
                    vec3 position;
                    float near;
                    float far;
                    ivec2 viewport;
                    float aspect;
                };""");
        GLUtils.checkErrorThrowing();

        camera = new PerspectiveCamera(window.getSize().getVec())
                .setOrbit(true)
                .setOrbitDistance(2)
                .setZoom(7);
        entitySystem.addService(new DirectionalBillBoardShadowMappingService(camera));
        entitySystem.addService(new BillBoardRenderService(camera));
        entitySystem.addService(new SnippetsService());

        window.getCursor().disable();

        gridShader = new ShaderProgram() {
            @Override
            protected void init() {
                hasGeometryShader();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"Grid.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("position", Uniform.Type.VEC3);
                addUniform("camera", Uniform.Type.STRUCT);
            }
        };

        dummyVao = glGenVertexArrays();

        floorTexture = new Texture2D.FileBuilder("grass.png")
                .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST).build();
        floorShader = new ShaderProgram() {
            @Override
            protected void init() {
                disableStrictUniformChecking();
                hasGeometryShader();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"Floor.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("camera", Uniform.Type.STRUCT);
                addUniform("position", Uniform.Type.VEC3);
                addUniform("diffuse", Uniform.Type.SAMPLER2D);
                addUniform("dirShadowMap", Uniform.Type.SAMPLER2D);
                addUniform("dirLightCombined", Uniform.Type.MAT4);
            }
        };

        Entity dude = entitySystem.createEntity();
        dudeTransform = dude.addComponent(new Transform());
        dudeBillBoard = dude.addComponent(new BillBoard(
                new Texture2D.FileBuilder("dude.png")
                        .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST).build(),
                new Vector2f()
        ));
        window.addKeyInputs(new KeyCharacterController<>(dudeTransform.getPosition(), (delta, target, deltaPosition, speed) -> {
            deltaPosition.y = 0;
            if (!deltaPosition.equals(0, 0, 0)) {
                dudeWalking = true;
                deltaPosition.normalize();
            } else {
                dudeWalking = false;
            }

            deltaPosition.x = -deltaPosition.x;
            deltaPosition.rotateY(toRadians(camera.getYaw()));
            target.add(deltaPosition.mul((float) delta));
        }));
        window.addKeyInputs(Input.of(
                Input.bind(Keys.A).to(() -> dudeLooksRight = false),
                Input.bind(Keys.D).to(() -> dudeLooksRight = true)
        ));

        entitySystem.createEntity(id -> new Enemy(id, dudeTransform.getPosition()));

        Random random = new Random(69420);

        var bushTexture = getPixelTexture("bush1.png");
        for (int i = 0; i < 100; i++) {
            entitySystem.createEntity()
                    .withComponent(new Transform().setPosition(new Vector3f(random.nextFloat(-10, 10), 0, random.nextFloat(-10, 10))))
                    .withComponent(new BillBoard(
                            bushTexture,
                            new Vector2f(0.5f)
                    ));
        }

        var treeTexture = getPixelTexture("tree01.png");
        for (int i = 0; i < 50; i++) {
            entitySystem.createEntity()
                    .withComponent(new Transform().setPosition(new Vector3f(random.nextFloat(-10, 10), 0, random.nextFloat(-10, 10))))
                    .withComponent(new BillBoard(
                            treeTexture,
                            new Vector2f(2, 4),
                            true
                    ));
        }

        dirLight = entitySystem.createEntity()
                .addComponent(new DirectionalLightComponent(
                        new DirectionalLight(new Vector3f(1, -1, 1)),
                        DirectionalShadowMap.generate(new Vector2i(2048)),
                        new OrthographicCamera(new Vector2i(2048),
                                -10, 10, -10, 10)
                                .setPosition(new Vector3f(10))
                                .setRotation(-45, 150, 0)
                                .setFar(100)
                ));

        stoopidShader = new ShaderProgram() {
            @Override
            protected void init() {
                hasGeometryShader();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"Blit.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("tex", Uniform.Type.SAMPLER2D);
            }
        };
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
        camera.setRotation(-45, 0, 0);
        camera.setPosition(new Vector3f(dudeTransform.getPosition()).add(0, 0.5f, 0));

        dudeBillBoard.getSize().set(dudeLooksRight ? -.5f : .5f, .5f);
        float verticalOffset = dudeWalking ? (float) (0.075f * sin(20 * pacer.getTime()) + 0.075f) : 0;
        dudeBillBoard.getOffset().set(0, verticalOffset, 0);

        renderFloor(); //not in #render because floor needs to be drawn first
//        renderGrid();
    }

    @Override
    protected void render() {
//        stoopidShader.start();
//        stoopidShader.setTexture("tex", dirLight.getShadowMap().getTexture());
//
//        glBindVertexArray(dummyVao);
//
//        glDisable(GL_DEPTH_TEST);
//        glDrawArrays(GL_POINTS, 0, 1);
//        glEnable(GL_DEPTH_TEST);
    }

    private void renderFloor() {
        GL46C.glBindTextureUnit(0, 0);

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
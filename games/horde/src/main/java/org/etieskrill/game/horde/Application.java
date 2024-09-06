package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.KeyCharacterController;
import org.etieskrill.engine.window.Window;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.joml.Math.sin;
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

    Transform dudeTransform;
    BillBoard dudeBillBoard;
    boolean dudeWalking = false;
    boolean dudeLooksRight = true;

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
        camera = new PerspectiveCamera(window.getSize().getVec());
        entitySystem.addService(new BillBoardRenderService(camera));

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
            }
        };

        Entity dude = entitySystem.createEntity();
        dudeTransform = new Transform();
        dude.addComponent(dudeTransform);
        dudeBillBoard = new BillBoard(
                new Texture2D.FileBuilder("dude.png")
                        .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST).build(),
                new Vector2f()
        );
        dude.addComponent(dudeBillBoard);
        window.addKeyInputs(new KeyCharacterController<>(dudeTransform.getPosition(), (delta, target, deltaPosition, speed) -> {
            deltaPosition.y = 0;
            if (!deltaPosition.equals(0, 0, 0)) {
                dudeWalking = true;
                deltaPosition.normalize();
            } else {
                dudeWalking = false;
            }

            deltaPosition.z = -deltaPosition.z;
            deltaPosition.rotateY(toRadians(90 + camera.getYaw()));
            target.add(deltaPosition.mul((float) delta));
        }));
        window.addKeyInputs(Input.of(
                Input.bind(Keys.A).to(() -> dudeLooksRight = false),
                Input.bind(Keys.D).to(() -> dudeLooksRight = true)
        ));

        Entity bush = entitySystem.createEntity();
        bush.addComponent(new Transform().setPosition(new Vector3f(1, 0, -1)));
        bush.addComponent(new BillBoard(
                new Texture2D.FileBuilder("bush1.png")
                        .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST)
                        .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_EDGE)
                        .build(),
                new Vector2f(0.5f)
        ));
    }

    @Override
    protected void loop(double delta) {
        camera.setRotation(-45, 90, 0);
        camera.setPosition(new Vector3f(dudeTransform.getPosition())
                .sub(camera.getDirection().mul(4).add(0, -0.5f, 0))
        );

        dudeBillBoard.getSize().set(dudeLooksRight ? .5f : -.5f, .5f);
        float verticalOffset = dudeWalking ? (float) (0.075f * sin(20 * pacer.getTime()) + 0.075f) : 0;
        dudeBillBoard.getOffset().set(0, verticalOffset, 0);
    }

    @Override
    protected void render() {
        glBindVertexArray(dummyVao);

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        floorShader.setUniform("camera", camera);
        floorShader.setUniform("diffuse", 0);
        floorTexture.bind(0);
        floorShader.start();

        glDrawArrays(GL_POINTS, 0, 1);

//        gridShader.setUniform("camera", camera);
//        gridShader.start();
//
//        glDisable(GL_DEPTH_TEST);
//        glDepthMask(false);
//        glDrawArrays(GL_POINTS, 0, 1);
//        glDepthMask(true);
//        glEnable(GL_DEPTH_TEST);
    }

    public static void main(String[] args) {
        new Application();
    }

}
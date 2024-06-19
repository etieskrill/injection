package org.etieskrill.games.particles;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.RenderService;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.BufferObject;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.particle.Particle;
import org.etieskrill.engine.graphics.particle.ParticleEmitter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Type;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCharacterTranslationController;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.etieskrill.engine.window.Window.WindowMode.BORDERLESS;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL45C.glCreateVertexArrays;

public class Application extends GameApplication {

    Model grid;
    Camera camera;
    Vector3f viewCenter, viewCenterDelta;

    Label fpsLabel;

    ParticleEmitter fireEmitter;
    ShaderProgram fireShader;

    public Application() {
        super(60, new Window.Builder()
                .setTitle("Particles")
                .setMode(BORDERLESS)
                .setSamples(4)
                .setVSyncEnabled(true)
                .build()
        );
    }

    @Override
    protected void init() {
        camera = new PerspectiveCamera(window.getSize().toVec());
        camera.setOrientation(-25, 45, 0);

        viewCenter = new Vector3f();
        viewCenterDelta = new Vector3f();

        window.addCursorInputs(new CursorCameraController(camera));
        window.addKeyInputs(new KeyCharacterTranslationController(viewCenterDelta, camera, false));
        window.getCursor().disable();

        window.addKeyInputs(Input.of(
                Input.bind(Keys.C).to(() -> {
                    camera.setOrientation(-25, 45, 0);
                    viewCenter.zero();
                })
        ));

        fpsLabel = new Label("", Fonts.getDefault(36));
        window.setScene(new Scene(
                new Batch(renderer),
                new Container(fpsLabel),
                new OrthographicCamera(window.getSize().toVec())
        ));

        entitySystem.addService(new RenderService(renderer, camera, window.getSize().toVec()));

        Entity gridEntity = entitySystem.createEntity();
        gridEntity.addComponent(new Transform());

        grid = new Model.Builder("grid.obj")
                .disableCulling()
                .build();
        Drawable gridDrawable = new Drawable(grid);
        var gridShader = new ShaderProgram() {
            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"Grid.glsl"};
            }

            @Override
            protected void getUniformLocations() {
            }
        };
        gridDrawable.setShader(gridShader);
        gridEntity.addComponent(gridDrawable);

        fireEmitter = new ParticleEmitter(
                10000,
                2500,
                new Transform(),
                new Vector3f(0, 4, 0),
                4,
                new Vector4f(1, .75f, 0, 1),
                1,
                new Texture2D.FileBuilder("particles/fire_01_low_res.png", Type.DIFFUSE)
                        .setMipMapping(MinFilter.LINEAR, MagFilter.LINEAR)
                        .setWrapping(Wrapping.CLAMP_TO_BORDER).build(),
                new Vector3f(1)
        );
        fireShader = new ShaderProgram() {
            @Override
            protected void init() {
                disableStrictUniformChecking();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"ParticleFire.glsl"};
            }

            @Override
            protected void getUniformLocations() {
            }
        };

        glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);

        particleBuffer = BufferUtils.createByteBuffer(MAX_PARTICLES * PARTICLE_TRANSFER_BYTES);
        initParticleVAO();
    }

    private int particleVAO;
    private BufferObject particleVBO;

    private void initParticleVAO() {
        GLUtils.clearError();

        particleVAO = glCreateVertexArrays();
        glBindVertexArray(particleVAO);

        particleVBO = BufferObject.create(particleBuffer.capacity()).build();

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, PARTICLE_TRANSFER_BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, PARTICLE_TRANSFER_BYTES, PARTICLE_POSITION_BYTES);

        GLUtils.checkErrorThrowing();
    }

    public static final int PARTICLE_POSITION_BYTES = 3 * Float.BYTES;
    public static final int PARTICLE_COLOUR_BYTES = 4 * Float.BYTES;
    public static final int PARTICLE_TRANSFER_BYTES = PARTICLE_POSITION_BYTES + PARTICLE_COLOUR_BYTES;

    public static final int MAX_PARTICLES = 10000;

    private ByteBuffer particleBuffer;

    @Override
    protected void loop(double delta) {
        fpsLabel.setText(String.valueOf((int) pacer.getAverageFPS()));

        viewCenter.add(viewCenterDelta.mul((float) delta));
        camera.setPosition(camera.getDirection().negate().mul(5).add(viewCenter));

        fireEmitter.update(delta);

        renderer.bindNextFreeTexture(fireShader, "sprite", fireEmitter.getSprite());
        fireShader.setUniform("size", fireEmitter.getSize());
        fireShader.setUniform("view", camera.getView());
        fireShader.setUniform("perspective", camera.getPerspective());
        fireShader.setUniform("screenSize", window.getSize().toVec());
        particleBuffer.rewind().limit(fireEmitter.getAliveParticles().size() * PARTICLE_TRANSFER_BYTES);
        for (Particle particle : fireEmitter.getAliveParticles()) {
            particle.getPosition().get(particleBuffer).position(particleBuffer.position() + PARTICLE_POSITION_BYTES);

            Vector4f colour = particle.getColour();
            colour.y *= particle.getLifetime() / 4;
            colour.w *= particle.getLifetime() / 4;
            colour.get(particleBuffer).position(particleBuffer.position() + PARTICLE_COLOUR_BYTES);
        }
        if (particleBuffer.position() != fireEmitter.getAliveParticles().size() * PARTICLE_TRANSFER_BYTES)
            throw new IllegalStateException("Particle buffer position does not align with particle byte number");

        //TODO maybe try glMapBuffer at some point?
        particleVBO.setData(particleBuffer);

        glBindVertexArray(particleVAO);

//        renderer.render(
//                quad.getTransform(),
//                quad,
//                fireShader,
//                camera.getCombined()
//        );

        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_POINTS, 0, fireEmitter.getAliveParticles().size());
        glBlendFunc(GL_ONE, GL_ZERO);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);

        glBindVertexArray(0);
    }

    public static void main(String[] args) {
        new Application();
    }

}
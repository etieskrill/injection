package org.etieskrill.games.particles;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.common.Interpolator;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.RenderService;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.VertexArrayObject;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.particle.Particle;
import org.etieskrill.engine.graphics.particle.ParticleEmitter;
import org.etieskrill.engine.graphics.particle.ParticleRoot;
import org.etieskrill.engine.graphics.particle.ParticleVertexAccessor;
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
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;
import static org.etieskrill.engine.window.Window.WindowMode.BORDERLESS;
import static org.lwjgl.opengl.GL15C.*;

public class Application extends GameApplication {

    Model grid;
    Camera camera;
    Vector3f viewCenter, viewCenterDelta;

    Label fpsLabel;

    ParticleEmitter fireEmitter;

    ParticleEmitter riftSmokeEmitter;
    ParticleEmitter riftSparkEmitter;
    ParticleEmitter riftShineEmitter;

    ShaderProgram particleShader;

    VertexArrayObject<Particle> particleVAO;

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

        fireEmitter = ParticleEmitter.builder(
                        4,
                        new Texture2D.FileBuilder("particles/fire_01.png", Type.DIFFUSE)
                                .setMipMapping(MinFilter.LINEAR, MagFilter.LINEAR)
                                .setWrapping(Wrapping.CLAMP_TO_BORDER).build())
                .particlesPerSecond(2500)
                .transform(new Transform().setPosition(new Vector3f(6, 0, 0)))
                .velocity(() -> new Vector3f(0, 4, 0))
                .colour(new Vector4f(1, .75f, 0, 1))
                .scatter(new Vector3f(1))
                .build();

        riftSparkEmitter = ParticleEmitter.builder(
                        .05f,
                        new Texture2D.FileBuilder("particles/spark_04.png", Type.DIFFUSE).build())
                .lifetimeSpread(.05f)
                .particlesPerSecond(10)
                .particleDelaySpreadSeconds(1)
                .colour(.85f, .6f, .85f)
                .size(2)
                .scatter(.5f, 1.5f, .5f)
                .maxScatterAngle(360)
                .build();

        riftShineEmitter = ParticleEmitter.builder(
                        1,
                        new Texture2D.FileBuilder("particles/star_01.png", Type.DIFFUSE).build())
                .particlesPerSecond(.5f)
                .colour(1, .75f, 1, .5f)
                .updateAlphaFunction(Interpolator.SMOOTHSTEP)
                .size(5)
                .build();

        riftSmokeEmitter = ParticleEmitter.builder(
                        1,
                        new Texture2D.FileBuilder("particles/smoke_05.png", Type.DIFFUSE).build())
                .particlesPerSecond(500)
                .randomVelocity(2)
                .colour(new Vector4f(.15f, 0, .25f, .5f))
                .updateAlphaFunction(Interpolator.QUADRATIC)
                .scatter(new Vector3f(.5f, 1.5f, .5f))
                .emitter(riftSparkEmitter)
                .emitter(riftShineEmitter)
                .build();

        particleShader = new ShaderProgram() {
            @Override
            protected void init() {
                hasGeometryShader();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"ParticlePointVertex.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("model", MAT4);
                addUniform("camera", STRUCT);
                addUniform("size", FLOAT);
                addUniform("sprite", SAMPLER2D);
            }
        };

        particleVAO = VertexArrayObject
                .builder(ParticleVertexAccessor.getInstance())
                .vertexBufferByteSize(10000L * ParticleVertexAccessor.BYTE_SIZE)
                .build();
    }

    @Override
    protected void loop(double delta) {
        fpsLabel.setText(String.valueOf((int) pacer.getAverageFPS()));

        viewCenter.add(viewCenterDelta.mul((float) delta));
        camera.setPosition(camera.getDirection().negate().mul(5).add(viewCenter));

        fireEmitter.getTransform().getPosition()
                .set(cos(pacer.getTime()), 0, -sin(pacer.getTime()))
                .mul(6);

        if (new Vector3f(camera.getPosition()).sub(fireEmitter.getTransform().getPosition()).length() >
                new Vector3f(camera.getPosition()).sub(riftSmokeEmitter.getTransform().getPosition()).length()) {
            renderParticles(delta, fireEmitter, particleShader);
            renderParticles(delta, riftSmokeEmitter, particleShader);
        } else {
            renderParticles(delta, riftSmokeEmitter, particleShader);
            renderParticles(delta, fireEmitter, particleShader);
        }
    }

    private void renderParticles(double delta, ParticleRoot rootEmitter, ShaderProgram shader) {
        if (rootEmitter instanceof ParticleEmitter emitter) {
            renderParticles(delta, new Matrix4f(), emitter, shader);
        } else {
            for (ParticleEmitter childEmitter : rootEmitter.getEmitters()) {
                renderParticles(delta, new Matrix4f(rootEmitter.getTransform().getMatrix()), childEmitter, shader);
            }
        }
    }

    private void renderParticles(double delta, Matrix4fc transform, ParticleEmitter emitter, ShaderProgram shader) {
        emitter.update(delta);

        shader.setUniform("model", transform);
        shader.setUniform("camera", camera);
        shader.setUniform("size", emitter.getSize());
        renderer.bindNextFreeTexture(shader, "sprite", emitter.getSprite());

        particleVAO.setAll(emitter.getAliveParticles());
        particleVAO.bind();

        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_POINTS, 0, emitter.getAliveParticles().size());
        glBlendFunc(GL_ONE, GL_ZERO);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);

        VertexArrayObject.unbind();

        for (ParticleEmitter childEmitter : emitter.getEmitters()) {
            renderParticles(
                    delta,
                    transform.mul(emitter.getTransform().getMatrix(), new Matrix4f()),
                    childEmitter,
                    shader
            );
        }
    }

    public static void main(String[] args) {
        new Application();
    }

}

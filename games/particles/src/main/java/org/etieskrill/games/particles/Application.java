package org.etieskrill.games.particles;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.common.Interpolator;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.impl.ParticleRenderService;
import org.etieskrill.engine.entity.service.impl.RenderService;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.renderer.GLParticleRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.particle.ParticleEmitter;
import org.etieskrill.engine.graphics.particle.ParticleNode;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Type;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCameraController;
import org.etieskrill.engine.scene.Scene;
import org.etieskrill.engine.scene.component.Container;
import org.etieskrill.engine.scene.component.Label;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.etieskrill.engine.window.Window.WindowMode.BORDERLESS;

public class Application extends GameApplication {

    Model grid;
    Camera camera;
    Vector3f viewCenter, viewCenterDelta;

    Label fpsLabel;

    ParticleEmitter fireEmitter;
    Transform fireEmitterTransform;

    public Application() {
        super(Window.builder()
                .setTitle("Particles")
                .setMode(BORDERLESS)
                .setSamples(4)
                .setVSyncEnabled(true)
                .build()
        );
    }

    @Override
    protected void init() {
        camera = new PerspectiveCamera(window.getSize().getVec())
                .setOrbit(true)
                .setOrbitDistance(5);
        camera.setRotation(-25, 45, 0);

        viewCenter = new Vector3f();
        viewCenterDelta = new Vector3f();

        window.addCursorInputs(new CursorCameraController(camera));
        window.addKeyInputs(new KeyCameraController(camera));
        window.getCursor().disable();

        window.addKeyInputs(Input.of(
                Input.bind(Keys.C).to(() -> {
                    camera.setRotation(-25, 45, 0);
                    viewCenter.zero();
                })
        ));

        fpsLabel = new Label("", Fonts.getDefault(36));
        window.setScene(new Scene(
                new Batch(renderer),
                new Container(fpsLabel),
                new OrthographicCamera(window.getSize().getVec())
        ));

        entitySystem.addService(new RenderService(renderer, camera, window.getSize().getVec())
                .blur(false));
        entitySystem.addService(new ParticleRenderService(new GLParticleRenderer(), camera));

        Entity gridEntity = entitySystem.createEntity();
        gridEntity.withComponent(new Transform());

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
        gridEntity.withComponent(gridDrawable);

        fireEmitter = ParticleEmitter.builder(
                        4,
                        new Texture2D.FileBuilder("particles/fire_01.png")
                                .setMipMapping(MinFilter.LINEAR, MagFilter.LINEAR)
                                .setWrapping(Wrapping.CLAMP_TO_BORDER).build())
                .particlesPerSecond(2500)
                .velocity(() -> new Vector3f(0, 4, 0))
                .colour(new Vector4f(1, .75f, 0, 1))
                .scatter(new Vector3f(1))
                .build();
        fireEmitterTransform = fireEmitter.getTransform();
        entitySystem.createEntity(id -> new Entity(id)
                .withComponent(fireEmitter.getTransform())
                .withComponent(ParticleNode.builder().emitter(fireEmitter).build()));

        ParticleEmitter riftSmokeEmitter = ParticleEmitter.builder(
                        1,
                        new Texture2D.FileBuilder("particles/smoke_05.png").build())
                .particlesPerSecond(500)
                .randomVelocity(2)
                .colour(new Vector4f(.15f, 0, .25f, .5f))
                .updateAlphaFunction(Interpolator.QUADRATIC)
                .scatter(new Vector3f(.5f, 1.5f, .5f))
                .build();

        ParticleEmitter riftSparkEmitter = ParticleEmitter.builder(
                        .05f,
                        new Texture2D.FileBuilder("particles/spark_04.png").build())
                .lifetimeSpread(.05f)
                .particlesPerSecond(10)
                .particleDelaySpreadSeconds(1)
                .colour(.85f, .6f, .85f)
                .size(2)
                .scatter(.5f, 1.5f, .5f)
                .maxScatterAngle(360)
                .build();

        ParticleEmitter riftShineEmitter = ParticleEmitter.builder(
                        1,
                        new Texture2D.FileBuilder("particles/star_01.png", Type.DIFFUSE).build())
                .particlesPerSecond(.5f)
                .colour(1, .75f, 1, .5f)
                .updateAlphaFunction(Interpolator.SMOOTHSTEP)
                .size(5)
                .build();

        ParticleNode riftParticles = ParticleNode.builder()
                .emitter(riftSmokeEmitter)
                .emitter(riftSparkEmitter)
                .emitter(riftShineEmitter)
                .build();
        entitySystem.createEntity(id -> new Entity(id)
                .withComponent(riftParticles.getTransform())
                .withComponent(riftParticles));
    }

    @Override
    protected void loop(double delta) {
        fpsLabel.setText(String.valueOf((int) pacer.getAverageFPS()));

        fireEmitterTransform.getPosition()
                .set(cos(pacer.getTime()), 0, -sin(pacer.getTime()))
                .mul(6);
    }

    @Override
    protected void terminate() {
        grid.dispose();
        super.terminate();
    }

    public static void main(String[] args) {
        new Application();
    }

}

package org.etieskrill.games.particles;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.RenderService;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCharacterTranslationController;
import org.etieskrill.engine.window.Window;
import org.joml.Vector3f;

import static org.etieskrill.engine.window.Window.WindowMode.BORDERLESS;

public class Application extends GameApplication {

    Model grid;
    Camera camera;
    Vector3f viewCenter, viewCenterDelta;

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
    }

    @Override
    protected void loop(double delta) {
        viewCenter.add(viewCenterDelta.mul((float) delta));
        camera.setPosition(camera.getDirection().negate().mul(5).add(viewCenter));
    }

    public static void main(String[] args) {
        new Application();
    }

}
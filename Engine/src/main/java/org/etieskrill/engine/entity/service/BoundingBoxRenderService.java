package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.WorldSpaceAABB;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.Shaders.WireframeShader;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

public class BoundingBoxRenderService implements Service {

    private final Renderer renderer;
    private final WireframeShader shader;
    private final Camera camera;

    private boolean renderBoundingBoxes;

    private Model box;
    private final Transform boundingBoxTransform = new Transform();

    public BoundingBoxRenderService(Renderer renderer, Camera camera) {
        this.renderer = renderer;
        this.shader = new WireframeShader();
        this.camera = camera;

        this.renderBoundingBoxes = true;
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(WorldSpaceAABB.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        if (!renderBoundingBoxes) return;
        renderBoundingBox(targetEntity.getComponent(WorldSpaceAABB.class));
    }

    private void renderBoundingBox(WorldSpaceAABB worldSpaceBoundingBox) {
        if (box == null) {
            box = ModelFactory.box(new Vector3f(1));
        }

        renderer.renderWireframe(
                boundingBoxTransform
                        .setPosition(worldSpaceBoundingBox.getCenter())
                        .setScale(worldSpaceBoundingBox.getSize()),
                box,
                shader,
                camera
        );
    }

    public void setRenderBoundingBoxes(boolean renderBoundingBoxes) {
        this.renderBoundingBoxes = renderBoundingBoxes;
    }

    public void toggleRenderBoundingBoxes() {
        renderBoundingBoxes = !renderBoundingBoxes;
    }

    @Override
    public Set<Class<? extends Service>> runAfter() {
        return Set.of(BoundingBoxService.class);
    }

}

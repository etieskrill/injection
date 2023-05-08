package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import org.etieskrill.engine.math.Vec3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Literally a patchwork wrapper, should perhaps be replaced by a <code>merge</code> or <code>add</code>
 * method or something of the likes in {@link org.etieskrill.engine.graphics.gl.RawModel}.
 */
@Deprecated
public class RawModelList extends RawModel {

    private final List<RawModel> models;

    private Vec3 position = new Vec3();
    private float scale = 0f;
    private float rotation = 0f;
    private Vec3 rotationAxis = new Vec3();

    private Mat4 transform;

    public RawModelList() {
        super();
        this.models = new ArrayList<>();
    }

    public void add(RawModel model) {
        models.add(model);
    }

    public boolean remove(RawModel model) {
        return models.remove(model);
    }

    public void setPosition(Vec3 newPosition) {
        this.position.set(newPosition);
        updateTransform();
    }

    public void setScale(float scale) {
        this.scale = scale;
        updateTransform();
    }

    public void setRotation(float rotation, Vec3 rotationAxis) {
        this.rotation = rotation;
        this.rotationAxis = rotationAxis;
        updateTransform();
    }

    private void updateTransform() {
        transform.set(transform.identity().translate(position).scale(scale).rotate(rotation, rotationAxis));
    }

    public void render(Renderer renderer) {
        for (RawModel model : models) renderer.render(model);
    }

}

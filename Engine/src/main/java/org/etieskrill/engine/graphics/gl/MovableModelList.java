package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.math.Vec3f;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Literally a patchwork wrapper, should perhaps be replaced by a <code>merge</code> or <code>add</code>
 * method or something of the likes in {@link org.etieskrill.engine.graphics.gl.RawModel}.
 */
@Deprecated
public class MovableModelList {

    private final List<MovableModel> models;
    private final Vec3f position;

    public MovableModelList() {
        this.models = new ArrayList<>();
        this.position = new Vec3f();
    }

    public void add(RawModel model) {
        MovableModel movableModel = model instanceof MovableModel ? (MovableModel) model : new MovableModel(model);
        models.add(movableModel);
    }

    public boolean remove(RawModel model) {
        MovableModel movableModel = model instanceof MovableModel ? (MovableModel) model : new MovableModel(model);
        return models.remove(movableModel);
    }

    public Vec3f getPosition() {
        return position;
    }

    public synchronized void updatePosition(Vec3f newPosition) {
        this.position.set(newPosition);

        for (MovableModel model : models) {
            model.updatePosition(model.getPosition().add(newPosition));
        }
    }

    public void render(Renderer renderer) {
        for (MovableModel model : models) renderer.render(model);
    }

}

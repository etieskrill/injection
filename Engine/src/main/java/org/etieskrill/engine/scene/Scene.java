package org.etieskrill.engine.scene;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.input.CursorInputAdapter;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.scene.component.Node;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import static org.lwjgl.opengl.GL11C.*;

public class Scene implements CursorInputAdapter {

    private @NotNull Batch batch;
    private @NotNull Node root;
    private @NotNull Camera camera;

    private final Vector2f size;

    /**
     * Available for more convenient construction in subclasses. Take care to set {@link Scene#batch},
     * {@link Scene#root} and {@link Scene#camera} while still in the constructor. Use at your own peril.
     */
    @SuppressWarnings("DataFlowIssue")
    protected Scene() {
        this.batch = null;
        this.root = null;
        this.camera = null;

        this.size = new Vector2f(0);
    }

    public Scene(@NotNull Batch batch, @NotNull Node root, @NotNull Camera camera) {
        this.batch = batch;
        this.root = root;
        this.camera = camera;

        this.size = new Vector2f(0);

        camera.setOrientation(0f, -90f, 0f);
    }

    public void update(double delta) {
        root.getPosition().set(0);
        root.getSize().set(size);
        root.update(delta);

        root.format();
    }

    public void render() {
        if (!root.isVisible()) return;
        batch.setCombined(camera.getCombined());

        glDisable(GL_DEPTH_TEST); //TODO either this or implement with depth testing
        root.render(batch);
        glEnable(GL_DEPTH_TEST);
    }

    public @NotNull Batch getBatch() {
        return batch;
    }

    public void setBatch(@NotNull Batch batch) {
        this.batch = batch;
    }

    public @NotNull Node getRoot() {
        return root;
    }

    public void setRoot(@NotNull Node root) {
        this.root = root;
    }

    public @NotNull Camera getCamera() {
        return camera;
    }

    public void setCamera(@NotNull Camera camera) {
        this.camera = camera.setOrientation(0f, -90f, 0f);
    }

    public void show() {
        this.root.show();
    }

    public void hide() {
        this.root.hide();
    }

    public void setSize(Vector2fc size) {
        this.size.set(size);
        root.invalidate();
    }

    @Override
    public boolean invokeClick(Key button, int action, double posX, double posY) {
        return root.hit(button, action, posX, posY);
    }

}

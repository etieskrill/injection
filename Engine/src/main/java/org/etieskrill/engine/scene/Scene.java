package org.etieskrill.engine.scene;

import glm_.vec2.Vec2;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.input.CursorInputHandler;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.scene.component.Node;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.opengl.GL11C.*;

public class Scene implements CursorInputHandler {
    
    private Batch batch;
    private Node root;
    private Camera camera;

    private final Vec2 size;
    
    public Scene(Batch batch, Node root, Camera camera) {
        this.batch = requireNonNull(batch);
        this.root = requireNonNull(root);
        this.camera = requireNonNull(camera);
        
        this.size = new Vec2(0);
    
        camera.setOrientation(0f, -90f, 0f);
    }
    
    public void update(double delta) {
        root.getPosition().put(0);
        root.getSize().put(size);
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
    
    public Batch getBatch() {
        return batch;
    }
    
    public void setBatch(@NotNull Batch batch) {
        this.batch = batch;
    }
    
    public Node getRoot() {
        return root;
    }
    
    public void setRoot(@NotNull Node root) {
        this.root = root;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public void setCamera(@NotNull Camera camera) {
        this.camera = camera;
    }
    
    public void show() {
        this.root.show();
    }
    
    public void hide() {
        this.root.hide();
    }

    public void setSize(Vec2 size) {
        this.size.put(size);
        if (root != null) root.invalidate();
    }

    @Override
    public boolean invokeClick(Key button, int action, double posX, double posY) {
        return root.hit(button, action, posX, posY);
    }

    @Override
    public boolean invokeMove(double deltaX, double deltaY) {
        return false;
    }

    @Override
    public boolean invokeScroll(double deltaX, double deltaY) {
        return false;
    }

}
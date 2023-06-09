package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

public class Stage {
    
    private Batch batch;
    private Container root;
    private Camera camera;

    private Vec2f size;
    
    public Stage(Batch batch, Container root, Camera camera) {
        this.batch = batch;
        this.root = root;
        this.camera = camera;
    
        camera.setOrientation(0f, 90f, 0f);
    }
    
    public void update(double delta) {
        //TODO viewport based stuff
        root.position.set(0f, 0f);
        root.size.set(size);
        root.getLayout().setMinSize(size).setPrefSize(size);
        root.rotation = 0f;
        
        //camera.setScaleX(0.5f);
        //camera.setScaleX(0.5f);
    
        root.layout();
    }
    
    public void render() {
        if (!root.isVisible()) return;
        //batch.setTransform(new Mat4().scale(0.01f));
        batch.setCombined(camera.getCombined());
        root.render(batch);
        batch.resetTransform();
    }
    
    public Batch getBatch() {
        return batch;
    }
    
    public void setBatch(Batch batch) {
        this.batch = batch;
    }
    
    public Container getRoot() {
        return root;
    }
    
    public void setRoot(Container root) {
        this.root = root;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    public void show() {
        this.root.show();
    }
    
    public void hide() {
        this.root.hide();
    }

    public void setSize(Vec2f size) {
        this.size = size;
    }

}

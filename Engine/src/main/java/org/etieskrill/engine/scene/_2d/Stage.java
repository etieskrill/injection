package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.gl.Batch;

public class Stage {
    
    private Batch batch;
    private Container root;
    private Camera camera;
    
    public Stage(Batch batch, Container root, Camera camera) {
        this.batch = batch;
        this.root = root;
        this.camera = camera;
    }
    
    public void update(double delta) {
        //TODO viewport based stuff
        //root.setPosition(position);
        //root.setSize(size.getSize());
        //root.setRotation(0);
        
        camera.setScaleX(0.5f);
        camera.setScaleX(0.5f);
    
        root.layout();
    }
    
    public void render() {
        if (!root.isVisible()) return;
        //batch.setTransform(camera.getT);
        batch.setCombined(camera.getCombined());
        root.render(batch);
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
    
}

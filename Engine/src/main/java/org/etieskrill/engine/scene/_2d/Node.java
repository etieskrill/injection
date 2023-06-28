package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

public abstract class Node {
    
    protected boolean visible;
    
    public Node() {
        show();
    }
    
    protected abstract void draw(Batch batch);
    
    public void render(Batch batch) {
        if (!visible) return;
        draw(batch);
    }
    
    //public abstract void update(double delta);
    
    public boolean isVisible() {
        return visible;
    }
    
    public void show() {
        this.visible = true;
    }
    
    public void hide() {
        this.visible = false;
    }
    
}

package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

public abstract class Node {
    
    protected Node parent;
    
    protected final Vec2f position;
    protected final Vec2f size;
    protected float rotation;
    
    protected boolean visible;
    
    public Node() {
        this(new Vec2f());
    }
    
    public Node(Vec2f size) {
        this.position = new Vec2f();
        this.size = size;
        this.rotation = 0f;
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
    
    public Node getParent() {
        return parent;
    }
    
    protected void setParent(Node parent) {
        this.parent = parent;
    }
    
    public Vec2f getPosition() {
        return position;
    }
    
    public void setPosition(Vec2f position) {
        //float x = position.getX(), y = position.getY(), w = size.getX(), h = size.getY();
        //float pX = parent.position.getX(), pY = parent.position.getY(), pW = parent.getSize().getX(), pH = parent.getSize().getY();
        //if (position.getX() < parent.position.getX() || position.getY() >= 0 || )
        this.position.set(position);
    }
    
    public Vec2f getSize() {
        return size;
    }
    
    public void setSize(Vec2f size) {
        this.size.set(size);
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
    
}

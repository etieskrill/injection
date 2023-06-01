package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.input.Action;
import org.etieskrill.engine.math.Vec2f;

public abstract class Actor extends Node {
    
    protected final Vec2f position;
    protected final Vec2f size;
    protected float rotation;
    
    protected Action onClick, onHover, onDrag;
    
    public Actor() {
        this(new Vec2f(), new Vec2f(), 0f);
    }
    
    public Actor(Vec2f position, Vec2f size, float rotation) {
        this.position = position;
        this.size = size;
        this.rotation = rotation;
    }
    
    @Override
    public void draw(Renderer renderer, ModelFactory models) {}
    
    public Vec2f getPosition() {
        return position;
    }
    
    public void setPosition(Vec2f position) {
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

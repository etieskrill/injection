package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {
    
    protected final Vec2f position;
    protected final Vec2f size;
    protected float rotation;
    
    protected Node parent;
    protected final List<Node> children;
    
    protected boolean visible;
    
    public Node() {
        this(new Vec2f());
    }
    
    public Node(Vec2f size) {
        this.children = new ArrayList<>();
        this.position = new Vec2f();
        this.size = size;
        this.rotation = 0f;
        show();
    }
    
    protected abstract void draw(Batch batch);
    
    public void render(Batch batch) {
        if (!visible) return;
        draw(batch);
        children.forEach(child -> child.render(batch));
    }
    
    public abstract void update(double delta);
    
    public void addChild(Node child) {
        if (child == null) throw new IllegalArgumentException("child must not be null");
        if (child == this) throw new IllegalArgumentException("a node cannot be its own child");
        if (child.hasParent(this) || child.hasChild(this))
            throw new IllegalArgumentException("adding child would cause circular graph");
        child.setParent(this);
        children.add(child);
    }
    
    private boolean hasParent(Node node) {
        if (node.getParent() == null) return false;
        else if (this == node) return true;
        return node.getParent().hasParent(node);
    }
    
    private boolean hasChild(Node node) {
        if (children.size() == 0) return false;
        for (Node child : children) {
            if (child == node) return true;
            if (child.hasChild(node)) return true;
        }
        return false;
    }
    
    public boolean removeChild(Node child) {
        return children.remove(child);
    }
    
    public void clearChildren() {
        children.clear();
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void show() {
        this.visible = true;
        children.forEach(Node::show);
    }
    
    public void hide() {
        this.visible = false;
        children.forEach(Node::hide);
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

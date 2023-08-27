package org.etieskrill.engine.scene._2d;

import glm_.vec2.Vec2;
import org.jetbrains.annotations.Contract;

public abstract class LayoutNode extends Node implements Layoutable {
    
    protected LayoutNode parent;
    
    protected Layout layout;

    protected final Vec2 position, size;
    protected float rotation;
    
    protected boolean shouldLayout;
    
    public LayoutNode(Layout layout) {
        super();
        this.position = new Vec2();
        this.size = new Vec2();
        this.rotation = 0f;
        this.layout = layout;
        shouldLayout = true;
    }

    public LayoutNode() {
        this(Layout.get());
    }

    @Override
    public void invalidate() {
        shouldLayout = true;
    }
    
    /**
     * Mutates the argument, so a copy must be passed if this is not desired.
     *
     * @param position
     * @return
     */
    @Contract(mutates = "param1")
    public Vec2 getPositionRelativeToRoot(Vec2 position) {
        if (parent == null) return position.plus(this.position);
        return parent.getPositionRelativeToRoot(position.plus(this.position));
    }
    
    public Vec2 getPositionRelativeToRoot() {
        return getPositionRelativeToRoot(new Vec2(0f));
    }
    
    public LayoutNode getParent() {
        return parent;
    }
    
    protected void setParent(LayoutNode parent) {
        this.parent = parent;
    }
    
    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout.copy();
    }

    protected Vec2 getPosition() {
        return position;
    }

    protected void setPosition(Vec2 position) {
        this.position.put(position);
    }

    protected Vec2 getSize() {
        return size;
    }

    protected Vec2 setSize(Vec2 size) {
        this.size.put(size);
        return this.size;
    }

    protected float getRotation() {
        return rotation;
    }

    protected void setRotation(float rotation) {
        this.rotation = rotation;
    }

}

package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;
import org.jetbrains.annotations.Contract;

public abstract class LayoutNode extends Node implements Layoutable {
    
    protected LayoutNode parent;
    
    protected Layout layout;

    protected final Vec2f position, size;
    protected float rotation;
    
    protected boolean shouldLayout;
    
    public LayoutNode(Layout layout) {
        super();
        this.position = new Vec2f();
        this.size = new Vec2f();
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
    public Vec2f getPositionRelativeToRoot(Vec2f position) {
        if (parent == null) return position.add(this.position);
        return parent.getPositionRelativeToRoot(position.add(this.position));
    }
    
    public Vec2f getPositionRelativeToRoot() {
        return getPositionRelativeToRoot(new Vec2f(0f));
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

    protected Vec2f getPosition() {
        return position;
    }

    protected void setPosition(Vec2f position) {
        this.position.set(position);
    }

    protected Vec2f getSize() {
        return size;
    }

    protected void setSize(Vec2f size) {
        this.size.set(size);
    }

    protected float getRotation() {
        return rotation;
    }

    protected void setRotation(float rotation) {
        this.rotation = rotation;
    }

}

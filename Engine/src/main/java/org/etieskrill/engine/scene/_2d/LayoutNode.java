package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;

public abstract class LayoutNode extends Node implements Layoutable {
    
    private Layout layout;

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

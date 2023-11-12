package org.etieskrill.engine.scene.component;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public abstract class Node {
    
    private final Vec2 position; //property that is set internally by the format
    private final Vec2 size; //set by the user
    
    private Alignment alignment;
    private final Vec4 margin; //Swizzle: top, bottom, left, right
    private boolean visible;
    
    private Node parent;
    
    protected boolean shouldFormat;
    
    public enum Alignment {
        TOP_LEFT, TOP, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
    }
    
    public Node() {
        this.position = new Vec2(0);
        this.size = new Vec2(100);
        this.alignment = Alignment.TOP_LEFT;
        this.margin = new Vec4(0);
        this.visible = true;
        this.parent = null;
        this.shouldFormat = true;
    }
    
    public void update(double delta) {}
    public void format() {}
    public void render(Batch batch) {}
    
    public Vec2 getPosition() {
        return position;
    }
    
    protected Node setPosition(@NotNull Vec2 position) {
        this.position.put(requireNonNull(position));
        return this;
    }

    public Vec2 getAbsolutePosition() {
        if (parent == null) return new Vec2(position);
        return position.plus(parent.getAbsolutePosition());
    }
    
    public Vec2 getSize() {
        return size;
    }
    
    public Node setSize(@NotNull Vec2 size) {
        this.size.put(requireNonNull(size));
        invalidate();
        return this;
    }
    
    public Alignment getAlignment() {
        return alignment;
    }
    
    public Node setAlignment(@NotNull Alignment alignment) { //what kotlin type safety does to a mf
        this.alignment = requireNonNull(alignment);
        return this;
    }
    
    public Vec4 getMargin() {
        return margin;
    }
    
    public Node setMargin(@NotNull Vec4 margin) {
        this.margin.put(requireNonNull(margin));
        return this;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
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

    Node setParent(@Nullable Node parent) {
        this.parent = parent;
        if (parent != null) invalidate();
        return this;
    }
    
    protected boolean shouldFormat() {
        if (shouldFormat) shouldFormat = false;
        else return false;
        return true;
    }
    
    public void invalidate() {
        this.shouldFormat = true;
        if (parent != null) parent.invalidate();
    }

    //TODO consider turning scene into entity system to alleviate this responsibility from nodes
    public abstract boolean hit(Key button, int action, double posX, double posY);

    final boolean doesHit(double posX, double posY) {
        Vec2 absPos = getAbsolutePosition();
        return absPos.getX() + getSize().getX() >= posX && posX >= absPos.getX() &&
                absPos.getY() + getSize().getY() >= posY && posY >= absPos.getY();
    }
    
}

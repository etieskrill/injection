package org.etieskrill.engine.scene.component;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public abstract class Node {
    
    private final Vector2f position; //property that is set internally by the format
    private final Vector2f size; //set by the user
    
    private Alignment alignment;
    private final Vector4f margin; //Swizzle: top, bottom, left, right
    private boolean visible;
    
    private Node parent;
    
    protected boolean shouldFormat;
    
    public enum Alignment {
        TOP_LEFT, TOP, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
    }
    
    public Node() {
        this.position = new Vector2f(0);
        this.size = new Vector2f(100);
        this.alignment = Alignment.TOP_LEFT;
        this.margin = new Vector4f(0);
        this.visible = true;
        this.parent = null;
        this.shouldFormat = true;
    }
    
    public void update(double delta) {}
    public void format() {}
    public void render(Batch batch) {}
    
    public Vector2f getPosition() {
        return position;
    }
    
    protected Node setPosition(@NotNull Vector2f position) {
        this.position.set(requireNonNull(position));
        return this;
    }

    public Vector2f getAbsolutePosition() {
        if (parent == null) return new Vector2f(position);
        return new Vector2f(position).add(parent.getAbsolutePosition());
    }
    
    public Vector2f getSize() {
        return size;
    }
    
    public Node setSize(@NotNull Vector2f size) {
        this.size.set(requireNonNull(size));
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
    
    public Vector4f getMargin() {
        return margin;
    }
    
    public Node setMargin(@NotNull Vector4f margin) {
        this.margin.set(requireNonNull(margin));
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

    protected final boolean shouldFormat() {
        if (shouldFormat) shouldFormat = false;
        else return false;
        return true;
    }

    public final void invalidate() {
        this.shouldFormat = true;
        if (parent != null) parent.invalidate();
    }

    //TODO consider turning scene into entity system to alleviate this responsibility from nodes
    public abstract boolean hit(Key button, int action, double posX, double posY);

    final boolean doesHit(double posX, double posY) {
        Vector2f absPos = getAbsolutePosition();
        return absPos.x() + getSize().x() >= posX && posX >= absPos.x() &&
                absPos.y() + getSize().y() >= posY && posY >= absPos.y();
    }
    
}

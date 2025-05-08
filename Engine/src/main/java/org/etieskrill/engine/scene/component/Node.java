package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static java.util.Objects.requireNonNull;

public abstract class Node<T extends Node<T>> {

    private final Vector2f position; //property that is set internally by the format
    protected final Vector2f size; //set by the user

    private Alignment alignment;
    private final Vector4f margin; //Swizzle: top, bottom, left, right
    private boolean visible;

    private Node<?> parent;

    protected boolean shouldFormat;

    public enum Alignment {
        FIXED_POSITION,

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

    public void update(double delta) {
    }

    public void format() {
    }

    public void render(@NotNull Batch batch) {
    }

    public T setPosition(@NotNull Vector2f position) {
        this.position.set(requireNonNull(position));
        return (T) this;
    }

    public Vector2f getAbsolutePosition() {
        if (parent == null) return new Vector2f(position);
        return new Vector2f(position).add(parent.getAbsolutePosition());
    }

    public T setSize(@NotNull Vector2f size) {
        this.size.set(requireNonNull(size));
        invalidate();
        return (T) this;
    }

    public T setAlignment(@NotNull Alignment alignment) { //what kotlin type safety does to a mf
        this.alignment = requireNonNull(alignment);
        return (T) this;
    }

    public T setMargin(@NotNull Vector4f margin) {
        this.margin.set(requireNonNull(margin));
        return (T) this;
    }

    public void show() {
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    T setParent(@Nullable Node<?> parent) {
        this.parent = parent;
        if (parent != null) invalidate();
        return (T) this;
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

    public boolean hit(Key button, Keys.Action action, double posX, double posY) {
        return false;
    }

    final boolean doesHit(double posX, double posY) {
        Vector2f absPos = getAbsolutePosition();
        return absPos.x() + getSize().x() >= posX && posX >= absPos.x() &&
               absPos.y() + getSize().y() >= posY && posY >= absPos.y();
    }

    //primary mouse key is just implied for now TODO detect any mouse key (do keyboard keys make sense?)
    public boolean drag(double deltaX, double deltaY, double posX, double posY) {
        return false;
    }

    public Vector2f getPosition() {
        return this.position;
    }

    public Vector2f getSize() {
        return this.size;
    }

    public Alignment getAlignment() {
        return this.alignment;
    }

    public Vector4f getMargin() {
        return this.margin;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Node<?> getParent() {
        return this.parent;
    }

}

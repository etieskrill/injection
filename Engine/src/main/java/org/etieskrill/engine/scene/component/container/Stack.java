package org.etieskrill.engine.scene.component.container;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.scene.component.Node;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.scene.component.LayoutUtilsKt.getPreferredNodePosition;

/**
 * A node with any number of children, whose layouts are respected independently of each other.
 */
public class Stack extends Node<Stack> {

    private final List<Node<?>> children = new ArrayList<>();

    public Stack() {
        this(new ArrayList<>());
    }

    public Stack(@NotNull List<Node<?>> children) {
        addChildren(children.toArray(new Node<?>[0]));
    }

    public Stack(@NotNull Node<?>... children) {
        addChildren(children);
    }

    @Override
    public void update(double delta) {
        children.forEach(child -> child.update(delta));
    }

    @Override
    public void computeFixedSizes() {
        if (!getShouldFormat()) return;

        for (Node<?> child : children) {
            child.computeFixedSizes();
        }

        switch (getScaleMode()) {
            case FIXED -> {
                setComputedFixedSize(true);
                getFormattedSize().set(getSize());
                layout();
            }
            case CONTENT -> {
                if (children.stream().anyMatch(child ->
                        child.getScaleMode() == ScaleMode.GROW
                        || !child.getComputedFixedSize()
                )) {
                    setComputedFixedSize(false);
                    return;
                }

                computeBoundingBox();
                setComputedFixedSize(true);
                layout();
            }
            case GROW -> setComputedFixedSize(false);
        }
    }

    /**
     * Guaranteed to be called only when all children have either fixed or computed size.
     */
    protected void computeBoundingBox() {
        Vector2f min = null;
        Vector2f max = null;

        for (Node<?> child : children) {
            if (min == null) {
                min = new Vector2f(child.getPosition());
                max = new Vector2f(child.getPosition()).add(child.getSize());
                continue;
            }

            min.min(child.getPosition());
            max.max(new Vector2f(child.getPosition()).add(child.getSize()));
        }

        if (min != null) {
            getFormattedSize().set(max.sub(min));
        } else {
            getFormattedSize().set(0f);
        }
    }

    @Override
    public void layout() {
        if (!shouldFormat()) return;

        if (getParent() == null && getScaleMode() != ScaleMode.FIXED)
            throw new IllegalStateException("Scale mode for root node must be FIXED");

        for (Node<?> child : children) {
            child.layout();
            switch (child.getScaleMode()) {
                case FIXED -> {
                }
                case CONTENT -> {
                    if (!child.getComputedFixedSize()) { //growing child inside
                        child.getFormattedSize().set(getFormattedSize());
                    }
                }
                case GROW -> child.getFormattedSize().set(getFormattedSize());
            }

            if (child.getAlignment() != Alignment.FIXED_POSITION)
                child.setPosition(getPreferredNodePosition(getFormattedSize(), child));
        }
    }

    @Override
    public void render(@NotNull Batch batch) {
        children.forEach(child -> child.render(batch));
    }

    public List<Node<?>> getChildren() {
        return children;
    }

    public Stack addChildren(@NotNull Node<?>... children) {
        List.of(children).forEach(child -> {
            child.setParent(this);
            this.children.add(child);
        });
        return this;
    }

    public Stack setChild(int index, @NotNull Node<?> child) {
        child.setParent(this);
        this.children.set(index, child);
        return this;
    }

    public Stack removeChildren(@NotNull Node<?>... children) {
        List.of(children).forEach(child -> {
            child.setParent(null);
            this.children.remove(child);
        });
        return this;
    }

    public Stack clearChildren() {
        children.forEach(child -> child.setParent(null));
        children.clear();
        return this;
    }

    @Override
    public boolean handleHit(@NotNull Key button, Keys.@NotNull Action action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        for (Node<?> child : children) {
            if (child.handleHit(button, action, posX, posY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleKey(@NotNull Key key, Keys.@NotNull Action action) {
        for (Node<?> child : children) {
            if (child.handleKey(key, action)) return true;
        }
        return false;
    }

    @Override
    public boolean handleHover(double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        for (Node<?> child : children) {
            if (child.handleHover(posX, posY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleDrag(double deltaX, double deltaY, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        for (Node<?> child : children)
            if (child.handleDrag(deltaX, deltaY, posX, posY)) return true;
        return false;
    }

}

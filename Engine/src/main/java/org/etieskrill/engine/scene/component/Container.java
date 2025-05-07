package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

/**
 * A node with a single child, whose {@link Node.Layout layout} it respects.
 */
public class Container extends Node {

    private Node child;
    private final Vector4f colour = new Vector4f(0);

    public Container(@NotNull Node child) {
        setChild(child);
    }

    @Override
    public void update(double delta) {
        if (child != null) child.update(delta);
    }

    @Override
    public void format() {
        if (!shouldFormat() || child == null) return;

        child.format();
        child.setPosition(getPreferredNodePosition(getSize(), child));
    }

    @Override
    public void render(@NotNull Batch batch) {
        if (colour.w != 0) batch.renderBox(new Vector3f(getPosition(), 0), new Vector3f(getSize(), 0), colour);
        if (child != null) child.render(batch);
    }

    public void setChild(@NotNull Node child) {
        invalidate();
        this.child = requireNonNull(child).setParent(this);
    }

    public Container setColour(@NotNull Vector4f colour) {
        this.colour.set(requireNonNull(colour));
        return this;
    }

    @Override
    public boolean hit(Key button, Keys.Action action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        return child.hit(button, action, posX, posY); //container itself is not hittable
    }

}

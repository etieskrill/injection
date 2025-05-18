package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

/**
 * A node with a single child.
 */
public class Container extends Node<Container> {

    protected @Nullable Node<?> child;

    public Container(@Nullable Node<?> child) {
        if (child != null) setChild(child);
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
        if (renderedColour.w != 0) batch.renderBox(
                new Vector3f(getPosition(), 0),
                new Vector3f(getSize(), 0),
                renderedColour
        );
        if (child != null) child.render(batch);
    }

    public void setChild(@NotNull Node<?> child) {
        invalidate();
        this.child = requireNonNull(child).setParent(this);
    }

    public void setColour(@Nullable Vector4f colour) {
        this.colour = colour;
        if (colour != null) this.renderedColour.set(colour);
    }

    @Override
    public boolean hit(Key button, Keys.Action action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        if (child == null) return false;
        return child.hit(button, action, posX, posY); //container itself is not hittable
    }

}

package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
        if (getRenderedColour().w != 0) batch.renderBox(
                new Vector3f(getPosition(), 0),
                new Vector3f(getSize(), 0),
                getRenderedColour()
        );
        if (child != null) child.render(batch);
    }

    public void setChild(@NotNull Node<?> child) {
        invalidate();
        requireNonNull(child).setParent(this);
        this.child = child;
    }

    @Override
    public boolean handleHit(@NotNull Key button, Keys.@NotNull Action action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        if (child == null) return false;
        return child.handleHit(button, action, posX, posY); //container itself is not hittable
    }

    @Override
    public boolean handleDrag(double deltaX, double deltaY, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        if (child == null) return false;
        return child.handleDrag(deltaX, deltaY, posX, posY);
    }

}

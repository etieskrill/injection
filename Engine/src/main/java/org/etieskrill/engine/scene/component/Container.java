package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

/**
 * A node with a single child, whose {@link Node.Layout layout} it respects.
 */
public class Container extends Node {
    
    private Node child;
    
    public Container() {}
    
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
    public void render(Batch batch) {
        if (child != null) child.render(batch);
    }
    
    public Node getChild() {
        return child;
    }
    
    public void setChild(@NotNull Node child) {
        invalidate();
        this.child = requireNonNull(child).setParent(this);
    }

    @Override
    public boolean hit(Key button, int action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        return child.hit(button, action, posX, posY); //container itself is not hittable
    }

}

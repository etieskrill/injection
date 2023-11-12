package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

/**
 * A node with multiple children, whose {@link Node.Layout layouts} are respected independently of each other.
 */
public class Stack extends Node {
    
    private final List<Node> children;
    
    public Stack() {
        this(new ArrayList<>());
    }
    
    public Stack(@NotNull List<Node> children) {
        this.children = new ArrayList<>(requireNonNull(children));
    }
    
    @Override
    public void update(double delta) {
        children.forEach(child -> child.update(delta));
    }
    
    @Override
    public void format() {
        if (!shouldFormat() || children.stream().noneMatch(child -> child.shouldFormat)) return;
        
        for (Node child : children) {
            child.format();
            child.setPosition(getPreferredNodePosition(getSize(), child));
        }
    }
    
    @Override
    public void render(Batch batch) {
        children.forEach(child -> child.render(batch));
    }

    protected List<Node> getChildren() {
        return children;
    }

    public Stack addChildren(@NotNull Node... children) {
        List.of(children).forEach(child -> this.children.add(child.setParent(child)));
        return this;
    }
    
    public Stack removeChildren(@NotNull Node... children) {
        List.of(children).forEach(child -> this.children.remove(child.setParent(null)));
        return this;
    }
    
    public Stack clearChildren() {
        children.forEach(child -> child.setParent(null));
        children.clear();
        return this;
    }

    @Override
    public boolean hit(Key button, int action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        for (Node child : children)
            if (child.hit(button, action, posX, posY)) return true;
        return false;
    }

}

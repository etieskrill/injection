package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.Batch;

import java.util.ArrayList;
import java.util.List;

public abstract class Group extends LayoutNode {
    
    protected final List<Group> children;
    
    public Group() {
        super();
        this.children = new ArrayList<>();
    }
    
    @Override
    public void render(Batch batch) {
        if (!isVisible()) return;
        draw(batch);
        children.forEach(child -> child.render(batch));
    }
    
    public void addChild(Group child) {
        if (child.getParent() == this) return;
        if (child == this) throw new IllegalArgumentException("a group cannot be its own child");
        child.setParent(this);
        children.add(child);
    }
    
    public boolean removeChild(Group child) {
        if (!children.contains(child)) return false;
        child.setParent(null);
        return children.remove(child);
    }
    
    public void clearChildren() {
        children.clear();
    }
    
    @Override
    public void show() {
        super.show();
        if (children != null) children.forEach(Node::show);
    }
    
    @Override
    public void hide() {
        super.hide();
        children.forEach(Node::hide);
    }
    
}

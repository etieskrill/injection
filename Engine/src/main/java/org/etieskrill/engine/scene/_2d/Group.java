package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

public abstract class Group extends LayoutNode {
    
    protected final List<Node> children;
    
    public Group(Vec2f size) {
        super(size);
        this.children = new ArrayList<>();
    }
    
    @Override
    public void render(Batch batch) {
        if (!super.visible) return;
        super.render(batch);
        children.forEach(child -> child.render(batch));
    }
    
    public void addChild(Group child) {
        if (child.getParent() == this) return;
        if (child == this) throw new IllegalArgumentException("a group cannot be its own child");
        //if (child.hasParent(this) || child.hasChild(this))
        //    throw new IllegalArgumentException("adding child would cause circular graph");
        child.setParent(this);
        children.add(child);
    }
    
    /*private boolean hasParent(Group group) {
        if (group.getParent() == null) return false;
        else if (this == group) return true;
        return group.getParent().hasParent(group);
    }
    
    private boolean hasChild(Group group) {
        if (children.size() == 0) return false;
        for (Node child : children) {
            if (child == group) return true;
            if (child.hasChild(group)) return true;
        }
        return false;
    }*/
    
    public boolean removeChild(Node child) {
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

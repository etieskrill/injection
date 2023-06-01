package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {
    
    protected abstract void draw(Renderer renderer, ModelFactory models);
    
    public void render(Renderer renderer, ModelFactory models) {
        if (!visible) return;
        draw(renderer, models);
        children.forEach(child -> child.render(renderer, models));
    }
    
    protected final List<Node> children;
    
    protected boolean visible;
    
    protected Node() {
        this.children = new ArrayList<>();
        show();
    }
    
    public void addChild(Node child) {
        if (child == this) throw new IllegalArgumentException("a node cannot be its own child");
        children.add(child);
    }
    
    public boolean removeChild(Node child) {
        return children.remove(child);
    }
    
    public void clearChildren() {
        children.clear();
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void show() {
        this.visible = true;
        children.forEach(Node::show);
    }
    
    public void hide() {
        this.visible = false;
        children.forEach(Node::hide);
    }
    
}

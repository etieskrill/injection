package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.Renderer;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {
    
    public abstract void draw(Renderer renderer, ModelFactory models);
    
    public void render(Renderer renderer, ModelFactory models) {
        draw(renderer, models);
        children.forEach(child -> child.render(renderer, models));
    }
    
    private final List<Node> children;
    
    protected Node() {
        this.children = new ArrayList<>();
    }
    
    public void addChild(Node child) {
        children.add(child);
    }
    
    public boolean removeChild(Node child) {
        return children.remove(child);
    }
    
    public void clearChildren() {
        children.clear();
    }
    
}

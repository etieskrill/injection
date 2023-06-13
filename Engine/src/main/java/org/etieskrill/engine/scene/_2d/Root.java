package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

import static org.etieskrill.engine.scene._2d.Layout.Alignment.TOP_LEFT;

public class Root extends LayoutNode {
    
    public Root() {
        super(new Vec2f());
    }
    
    @Override
    protected void draw(Batch batch) {
    }
    
    @Override
    public void update(double delta) {
        children.forEach(child -> child.update(delta));
    }
    
    @Override
    public void layout() {
        if (!shouldLayout) return;
        
        //TODO implement multinode
        if (children.get(0) == null) return;
        
        if (children.get(0) instanceof LayoutNode child) {
            Vec2f newPosition = switch (child.getLayout().getAlignment()) {
                case TOP_LEFT -> new Vec2f(0f, size.getY() - child.getSize().getY());
                case TOP_CENTER -> new Vec2f(size.getX() / 2f - child.getSize().getX() / 2f, size.getY() - child.getSize().getY());
                case CENTER -> new Vec2f(size.getX() / 2f - child.getSize().getX() / 2f, size.getY() / 2f - child.getSize().getY() / 2f);
                case BOTTOM_LEFT -> new Vec2f(0f);
                case TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_CENTER, CENTER_RIGHT, CENTER_LEFT -> new Vec2f();
            };
            
            child.setPosition(newPosition);
        }
        
        shouldLayout = false;
    }
    
}

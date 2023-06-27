package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

public class Container extends LayoutNode {
    
    private Group child;
    
    public Container() {
        super(new Vec2f());
    }
    
    @Override
    protected void draw(Batch batch) {}
    
    @Override
    public void render(Batch batch) {
        if (!isVisible() || child == null) return;
        child.render(batch);
    }
    
    //@Override
    //public void update(double delta) {
    //    root.children.forEach(child -> child.update(delta));
    //}
    
    @Override
    public void layout() {
        if (!shouldLayout || child == null) return;
        
        child.layout();
        
        Vec2f newPosition = switch (getLayout().getAlignment()) {
            case TOP_LEFT -> new Vec2f(0f, size.getY() - child.getSize().getY());
            case TOP_CENTER -> new Vec2f(size.getX() / 2f - child.getSize().getX() / 2f, size.getY() - child.getSize().getY());
            case TOP_RIGHT -> new Vec2f(size.getX() - child.getSize().getX(), size.getY() - child.getSize().getY());
            case CENTER_LEFT -> new Vec2f(0f, size.getY() / 2f - child.getSize().getY() / 2f);
            case CENTER -> new Vec2f(size.getX() / 2f - child.getSize().getX() / 2f, size.getY() / 2f - child.getSize().getY() / 2f);
            case CENTER_RIGHT -> new Vec2f(size.getX() - child.getSize().getX(), size.getY() / 2f - child.getSize().getY() / 2f);
            case BOTTOM_LEFT -> new Vec2f(0f, 0f);
            case BOTTOM_CENTER -> new Vec2f(size.getX() / 2f - child.getSize().getX() / 2f, 0f);
            case BOTTOM_RIGHT -> new Vec2f(size.getX() - child.getSize().getX(), 0f);
        };
    
        child.setPosition(newPosition);
        
        shouldLayout = false;
    }
    
    @Override
    public void show() {
        this.visible = true;
        if (child != null) child.show();
    }
    
    @Override
    public void hide() {
        this.visible = false;
        if (child != null) child.hide();
    }
    
    public Group getChild() {
        return child;
    }
    
    public void setChild(Group child) {
        this.child = child;
    }
    
}

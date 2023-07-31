package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.math.Vec2f;

public class Container extends LayoutNode {
    
    private Group child;
    
    public Container() {
        super();
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
    public Vec2f computeSize(Vec2f minSize, Vec2f prefSize) {
        if (parent == null) return setSize(getLayout().getPrefSize());
        if (!getLayout().caresAboutSize()) return new Vec2f();
        
        return setSize(parent != null ? getLayout().getSize(parent.getSize()) : getLayout().getPrefSize());
    }
    
    @Override
    public void layout() {
        if (!shouldLayout || child == null) return;
        
        //child.computeSize();
        Vec2f computedSize = new Vec2f(child.getSize()), actualSize = new Vec2f(computedSize);
        Vec2f prefSize = getLayout().getPrefSize();

        double actualX = Math.min(child.getLayout().getPrefSize().getX(), Math.max(child.getLayout().getMinSize().getX(), computedSize.getX()));
        double actualY = Math.min(child.getLayout().getPrefSize().getY(), Math.max(child.getLayout().getMinSize().getY(), computedSize.getY()));
        if (computedSize.getX() > prefSize.getX()) actualSize.setX(Math.max(computedSize.getX(), getLayout().getMinSize().getX()));
        if (computedSize.getY() > prefSize.getY()) actualSize.setY(Math.max(computedSize.getY(), getLayout().getMinSize().getY()));
    
        //System.out.println(computedSize + " " + actualX + " " + actualY + " " + actualSize);

        Vec2f newPosition = switch (getLayout().getAlignment()) {
            case TOP_LEFT -> new Vec2f(0f, size.getY() - actualSize.getY());
            case TOP_CENTER -> new Vec2f(size.getX() / 2f - actualSize.getX() / 2f, size.getY() - actualSize.getY());
            case TOP_RIGHT -> new Vec2f(size.getX() - actualSize.getX(), size.getY() - actualSize.getY());
            case CENTER_LEFT -> new Vec2f(0f, size.getY() / 2f - actualSize.getY() / 2f);
            case CENTER -> new Vec2f(size.getX() / 2f - actualSize.getX() / 2f, size.getY() / 2f - actualSize.getY() / 2f);
            case CENTER_RIGHT -> new Vec2f(size.getX() - actualSize.getX(), size.getY() / 2f - actualSize.getY() / 2f);
            case BOTTOM_LEFT -> new Vec2f(0f, 0f);
            case BOTTOM_CENTER -> new Vec2f(size.getX() / 2f - actualSize.getX() / 2f, 0f);
            case BOTTOM_RIGHT -> new Vec2f(size.getX() - actualSize.getX(), 0f);
        };
    
        child.setPosition(getPositionRelativeToRoot(newPosition));
        child.layout();

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
        child.setParent(this);
        this.child = child;
    }
    
}

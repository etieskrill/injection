package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;

public abstract class LayoutNode extends Node implements Layoutable {
    
    private final Layout layout;
    
    protected boolean shouldLayout;
    
    public LayoutNode(Vec2f size) {
        super(size);
        this.layout = Layout.get();
        shouldLayout = true;
    }
    
    @Override
    public void invalidate() {
        shouldLayout = true;
    }
    
    public Layout getLayout() {
        return layout;
    }
    
}

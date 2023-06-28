package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

public class VerticalGroup extends Group {
    
    public VerticalGroup() {
        super();
    }

    public VerticalGroup(Group... children) {
        super();
        for (Group child : children) addChild(child);
    }
    
    @Override
    public Vec2f computeSize() {
        return null;
    }
    
    @Override
    public void layout() {
        float minWidth = 0, minHeight = 0;
        
        for (Group child : children) {
            child.layout();
            Vec2f computedSize = child.getSize();
            
            if (computedSize.getX() > minWidth) minWidth = computedSize.getX();
            minHeight += computedSize.getY();
        }
        
        switch (getLayout().getAlignment()) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> {}
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> {}
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> {}
        }
        
        for (Group child : children) {
            child.setSize(new Vec2f(100f));
            child.setPosition(getPositionRelativeToRoot(new Vec2f(0f, 0f)));
        }
    
        System.out.println(getPositionRelativeToRoot());
        
        shouldLayout = false;
    }
    
    @Override
    protected void draw(Batch batch) {
    }
    
}

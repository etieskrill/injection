package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.math.Vec2f;

public class VerticalGroup extends Group {
    
    public VerticalGroup(Vec2f size) {
        super(size);
    }
    
    @Override
    public void layout() {
        float minWidth = 0, minHeight = 0;
        
        for (Node child : children) {
            if (child.getSize().getX() > minWidth) minWidth = child.getSize().getX();
            minHeight += child.getSize().getY();
        }
        
        switch (getLayout().getAlignment()) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> {}
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> {}
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> {}
        }
        
        shouldLayout = false;
    }
    
    @Override
    protected void draw(Batch batch) {}
    
}

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
        Vec2f totalSpan = new Vec2f(0f);
        float minTotalWidth = 0f, minTotalHeight = 0f,
                prefTotalWidth = 0f, prefTotalHeight = 0f;

        for (Group child : children) {
            child.layout();
            totalSpan.add(child.getLayout().computeSpan());
            Vec2f computedSize = child.getSize();

            if (computedSize.getX() > minTotalWidth) minTotalWidth = computedSize.getX();
            minTotalHeight += computedSize.getY();
        }
    }
    
    @Override
    public void layout() {
        switch (getLayout().getAlignment()) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> {}
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> {}
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> {}
        }
        
        for (Group child : children) {
            child.setPosition(getPositionRelativeToRoot(new Vec2f(child.getSize()).scl(-0.5f)));
        }

        shouldLayout = false;
    }
    
    @Override
    protected void draw(Batch batch) {
    }
    
}

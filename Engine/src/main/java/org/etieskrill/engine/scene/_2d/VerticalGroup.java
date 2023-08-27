package org.etieskrill.engine.scene._2d;

import glm_.vec2.Vec2;
import org.etieskrill.engine.graphics.Batch;

public class VerticalGroup extends Group {
    
    public VerticalGroup() {
        super();
    }

    public VerticalGroup(Group... children) {
        super();
        for (Group child : children) addChild(child);
    }
    
    @Override
    public Vec2 computeSize(Vec2 minSize, Vec2 prefSize) {
        if (getLayout().caresAboutSize())
            return setSize(getLayout().getSize(parent.getSize()));
        
        //for (Group child : children) child.computeSize();
        //System.out.println(setSize(new Vec2(500f)));
        return size;
    }
    
    @Override
    public void layout() {
        if (!shouldLayout || children.size() == 0) return;
    
        Vec2 totalSpan = new Vec2(0f);
        float minTotalWidth = 0f, minTotalHeight = 0f,
                prefTotalWidth = 0f, prefTotalHeight = 0f;
    
        for (Group child : children) {
            totalSpan.plusAssign(child.getLayout().computeSpan());
    
            Vec2 minSize = child.getLayout().getMinSize();
            if (minSize.getX() > minTotalWidth) minTotalWidth = minSize.getX();
            minTotalHeight += minSize.getY();
    
            Vec2 prefSize = child.getLayout().getPrefSize();
            if (prefSize.getX() > prefTotalWidth) prefTotalWidth = prefSize.getX();
            prefTotalHeight += prefSize.getY();
        }
    
        //setSize(getLayout().getSize());
        //System.out.println("span: " + totalSpan + " " + minTotalWidth + " " + minTotalHeight + " " + prefTotalWidth + " " + prefTotalHeight);
        
        switch (getLayout().getAlignment()) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> {}
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> {}
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> {}
        }
        
        for (Group child : children) {
            child.setPosition(getPositionRelativeToRoot(/*new Vec2(child.getSize()).scl(-0.5f)*/));
        }

        shouldLayout = false;
    }
    
    @Override
    protected void draw(Batch batch) {
    }
    
}

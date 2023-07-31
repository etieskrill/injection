package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.Batch;
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
    public Vec2f computeSize(Vec2f minSize, Vec2f prefSize) {
        if (getLayout().caresAboutSize())
            return setSize(getLayout().getSize(parent.getSize()));
        
        //for (Group child : children) child.computeSize();
        //System.out.println(setSize(new Vec2f(500f)));
        return size;
    }
    
    @Override
    public void layout() {
        if (!shouldLayout || children.size() == 0) return;
    
        Vec2f totalSpan = new Vec2f(0f);
        float minTotalWidth = 0f, minTotalHeight = 0f,
                prefTotalWidth = 0f, prefTotalHeight = 0f;
    
        for (Group child : children) {
            totalSpan.add(child.getLayout().computeSpan());
        
            Vec2f minSize = child.getLayout().getMinSize();
            if (minSize.getX() > minTotalWidth) minTotalWidth = minSize.getX();
            minTotalHeight += minSize.getY();
        
            Vec2f prefSize = child.getLayout().getPrefSize();
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
            child.setPosition(getPositionRelativeToRoot(/*new Vec2f(child.getSize()).scl(-0.5f)*/));
        }

        shouldLayout = false;
    }
    
    @Override
    protected void draw(Batch batch) {
    }
    
}

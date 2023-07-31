package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;

public interface Layoutable {
    
    Vec2f computeSize(Vec2f minSize, Vec2f prefSize);
    void layout();
    
    void invalidate();
    
}

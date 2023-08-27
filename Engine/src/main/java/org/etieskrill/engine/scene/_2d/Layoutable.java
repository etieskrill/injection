package org.etieskrill.engine.scene._2d;

import glm_.vec2.Vec2;

public interface Layoutable {
    
    Vec2 computeSize(Vec2 minSize, Vec2 prefSize);
    void layout();
    
    void invalidate();
    
}

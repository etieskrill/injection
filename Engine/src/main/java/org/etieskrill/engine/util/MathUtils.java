package org.etieskrill.engine.util;

import glm_.vec2.Vec2;

public class MathUtils {

    private MathUtils() {
    }

    public static Vec2 clamp(Vec2 val, Vec2 min, Vec2 max) {
        return val.min(max).max(min);
    }

}

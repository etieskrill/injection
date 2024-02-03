package org.etieskrill.engine.util;

import org.joml.Vector2f;

public class MathUtils {

    private MathUtils() {
    }

    public static Vector2f clamp(Vector2f val, Vector2f min, Vector2f max) {
        return val.min(max).max(min);
    }

}

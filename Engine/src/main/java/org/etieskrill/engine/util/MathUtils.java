package org.etieskrill.engine.util;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;
import org.lwjgl.assimp.AIMatrix4x4;

public class MathUtils {

    private MathUtils() {}

    public static Vec2 clamp(Vec2 val, Vec2 min, Vec2 max) {
        return val.min(max).max(min);
    }

    public static Mat4 toMat4(AIMatrix4x4 mat) {
        return new Mat4(new float[]{
                mat.a1(), mat.b1(), mat.c1(), mat.d1(),
                mat.a2(), mat.b2(), mat.c2(), mat.d2(),
                mat.a3(), mat.b3(), mat.c3(), mat.d3(),
                mat.a4(), mat.b4(), mat.c4(), mat.d4(),
        });
    }

}

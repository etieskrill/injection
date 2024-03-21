package org.etieskrill.engine.graphics.util;

import org.joml.Matrix4f;
import org.lwjgl.assimp.AIMatrix4x4;

public class AssimpUtils {

    public static Matrix4f fromAI(AIMatrix4x4 mat) {
        return new Matrix4f(
                mat.a1(), mat.b1(), mat.c1(), mat.d1(),
                mat.a2(), mat.b2(), mat.c2(), mat.d2(),
                mat.a3(), mat.b3(), mat.c3(), mat.d3(),
                mat.a4(), mat.b4(), mat.c4(), mat.d4()
        );
    }

}

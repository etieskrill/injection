package org.etieskrill.engine.graphics.util;

import net.bifreus.kml.Mat4;
import org.lwjgl.assimp.AIMatrix4x4;

public class AssimpUtils {

    public static Mat4 fromAI(AIMatrix4x4 mat) {
        return new Mat4(
                mat.a1(), mat.b1(), mat.c1(), mat.d1(),
                mat.a2(), mat.b2(), mat.c2(), mat.d2(),
                mat.a3(), mat.b3(), mat.c3(), mat.d3(),
                mat.a4(), mat.b4(), mat.c4(), mat.d4()
        );
    }

}

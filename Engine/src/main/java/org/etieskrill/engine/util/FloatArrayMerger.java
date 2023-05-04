package org.etieskrill.engine.util;

import java.nio.FloatBuffer;

public class FloatArrayMerger {

    public static float[] merge(float[] a1, float[] a2, int stride1, int stride2) {
        int numVertices = a1.length / stride1;

        System.out.println(numVertices + " " + a2.length / stride2);

        if (numVertices != a2.length / stride2)
            throw new IllegalArgumentException("Vertex and colour array length does not match");

        int totalSize = a1.length + a2.length;
        FloatBuffer buffer = FloatBuffer.wrap(new float[totalSize]);
        for (int i = 0; i < numVertices; i++) {
            for (int j = 0; j < stride1; j++)
                buffer.put(a1[i * stride1 + j]);
            for (int j = 0; j < stride2; j++)
                buffer.put(a2[i * stride2 + j]);
        }

        return buffer.array();
    }

}

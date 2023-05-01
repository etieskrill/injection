package org.etieskrill.injection.graphics.gl;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_FAN;

public class ShapeModelFactory {

    private final Loader loader = new Loader();

    public RawMemoryModel rectangle(float x, float y, float width, float height) {
        if (width < 0) {
            float tmp = x;
            x = width;
            width = tmp;
        }

        if (height < 0) {
            float tmp = y;
            y = height;
            height = tmp;
        }

        float[] vertices = {
                x, y,
                x + width, y,
                x, y + height,
                x + width, y + height
        };

        int[] indices = {0, 1, 2, 2, 1, 3};

        return loader.loadToVAO(vertices, indices, GL_TRIANGLES);
    }

    /**
     * asdf
     *
     * @param x horizontal position of circle sector centre
     * @param y vertical position of circle sector centre
     * @param radius circle sector radius
     * @param start circle sector start point in degrees
     * @param end circle sector end point in degrees
     * @param segments number of segments to subdivide the circle sector into
     * @return an indexed memory model of the circle sector
     */
    public RawMemoryModel circleSect(float x, float y, float radius, float start, float end, int segments) {
        if (radius < 0) throw new IllegalArgumentException("Radius should not be smaller than zero");
        if (segments < 3) throw new IllegalArgumentException("Circle sector must have more than two segments");

        FloatBuffer vertices = BufferUtils.createFloatBuffer(2 * (segments + 2));
        vertices.put(x).put(y);

        IntBuffer indices = BufferUtils.createIntBuffer(segments + 2);
        indices.put(0);

        for (int i = 0; i <= segments; i++) {
            float subAngle = i * ((end - start) / segments) + start;

            float subX = (float) (radius * Math.cos(Math.toRadians(subAngle)));
            float subY = (float) (radius * Math.sin(Math.toRadians(subAngle)));

            vertices.put(subX).put(subY);
            indices.put(i + 1);
        }

        return loader.loadToVAO(vertices.array(), indices.array(), GL_TRIANGLE_FAN);
    }

    public RawMemoryModel circle(float x, float y, float radius, int segments) {
        return circleSect(x, y, radius, 0, 360, segments);
    }

    public void disposeLoader() {
        loader.cleanup();
    }

}

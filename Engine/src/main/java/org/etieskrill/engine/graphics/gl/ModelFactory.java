package org.etieskrill.engine.graphics.gl;

import glm.vec._3.Vec3;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL33C;

public class ModelFactory {

    private final Loader loader = new Loader();

    public RawModel rectangle(float x, float y, float width, float height) {
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
                x, y, 0f,
                x + width, y, 0f,
                x, y + height, 0f,
                x + width, y + height, 0f
        };
        
        float[] colours = new float[] {
                1f, 0f, 0f, 1f,
                0f, 1f, 0f, 1f,
                0f, 0f, 1f, 1f,
                1f, 1f, 0f, 1f
        };

        float[] textures = new float[] {
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f
        };

        short[] indices = {0, 1, 2, 2, 1, 3};

        return loader.loadToVAO(vertices, colours, textures, indices, GL11C.GL_TRIANGLES);
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
    public RawModel circleSect(float x, float y, float radius, float start, float end, int segments) {
        if (radius < 0) throw new IllegalArgumentException("Radius should not be smaller than zero");
        if (segments < 3) throw new IllegalArgumentException("Circle sector must have more than two segments");

        FloatBuffer vertices = BufferUtils.createFloatBuffer(3 * (segments + 2));
        vertices.put(x).put(y).put(0f);

        ShortBuffer indices = BufferUtils.createShortBuffer(segments + 2);
        indices.put((short) 0);

        for (short i = 0; i <= segments; i++) {
            float subAngle = i * ((end - start) / segments) + start;

            float subX = (float) (radius * Math.cos(Math.toRadians(subAngle))) + x;
            float subY = (float) (radius * Math.sin(Math.toRadians(subAngle))) + y;

            vertices.put(subX).put(subY).put(0f);
            indices.put((short) (i + 1));
        }
        
        vertices.flip();
        float[] vertices_a = new float[vertices.capacity()];
        vertices.get(vertices_a);
    
        float[] colours = new float[(int) (vertices_a.length / 0.75)];
        Arrays.fill(colours, 1f);

        float[] textures = new float[(int) (vertices_a.length / 1.5)];
        Arrays.fill(textures, 0f);
        
        indices.flip();
        short[] indices_a = new short[indices.capacity()];
        indices.get(indices_a);
        
        return loader.loadToVAO(vertices_a, colours, textures, indices_a, GL11C.GL_TRIANGLE_FAN);
    }

    public RawModel circle(float x, float y, float radius, int segments) {
        return circleSect(x, y, radius, 0, 360, segments);
    }

    public RawModelList roundedRect(float x, float y, float width, float height, float rounding, int segments) {
        if (rounding < 0) throw new IllegalArgumentException("corner rounding cannot be smaller than zero");
        if (2 * rounding > width || 2 * rounding > height)
            throw new IllegalArgumentException("rounding cannot exceed total width or height");

        RawModelList models = new RawModelList();

        float xTopLeft = x + rounding, yTopLeft = y + height - rounding;
        float xTopRight = x + width - rounding, yTopRight = y + height - rounding;
        float xBottomLeft = x + rounding, yBottomLeft = y + rounding;
        float xBottomRight = x + width - rounding, yBottomRight = y + rounding;

        models.add(rectangle(x + rounding, y, width - 2 * rounding, height));
        models.add(rectangle(x, y + rounding, rounding, height - 2 * rounding));
        models.add(rectangle(x + width - rounding, y + rounding, rounding, height - 2 * rounding));
        models.add(circleSect(xTopLeft, yTopLeft, rounding, 90, 180, segments));
        models.add(circleSect(xTopRight, yTopRight, rounding, 0, 90, segments));
        models.add(circleSect(xBottomLeft, yBottomLeft, rounding, 180, 270, segments));
        models.add(circleSect(xBottomRight, yBottomRight, rounding, 270, 360, segments));

        return models;
    }

    public RawModel box(Vec3 size) {
        /*if (width < 0) {
            float tmp = x;
            x = width;
            width = tmp;
        }

        if (height < 0) {
            float tmp = y;
            y = height;
            height = tmp;
        }

        if (depth < 0) {
            float tmp = z;
            z = depth;
            depth = tmp;
        }*/

        /*float[] vertices = {
                0f, 0f, 0f,
                size.x, 0f, 0f,
                size.x, size.y, 0f,
                0f, size.y, 0f,
                0f, 0f, size.z,
                size.x, 0f, size.z,
                size.x, size.y, size.z,
                0f, size.y, size.z
        };

        float[] colours = new float[(int) (vertices.length / 0.75f)];

        float[] textures = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f,
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
        };

        short[] indices = {
                0, 1, 2, 0, 2, 3,
                0, 1, 5, 0, 5, 4,
                4, 5, 6, 4, 6, 7,
                4, 5, 6, 4, 6, 7
        };*/

        float[] vertices = {
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,

                -0.5f, -0.5f,  0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                -0.5f, -0.5f,  0.5f,

                -0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,

                0.5f,  0.5f,  0.5f,
                0.5f,  0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,

                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f, -0.5f,

                -0.5f,  0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                0.5f,  0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f, -0.5f
        };

        for (int i = 0; i < vertices.length; i+=3) {
            vertices[i] *= 2 * size.x;
            vertices[i + 1] *= 2 * size.y;
            vertices[i + 2] *= 2 * size.z;
        }

        float[] textures = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,

                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,

                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,

                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,

                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,

                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 1.0f
        };

        float[] normals = {
                0.0f,  0.0f, -1.0f,
                0.0f,  0.0f, -1.0f,
                0.0f,  0.0f, -1.0f,
                0.0f,  0.0f, -1.0f,
                0.0f,  0.0f, -1.0f,
                0.0f,  0.0f, -1.0f,

                0.0f,  0.0f, 1.0f,
                0.0f,  0.0f, 1.0f,
                0.0f,  0.0f, 1.0f,
                0.0f,  0.0f, 1.0f,
                0.0f,  0.0f, 1.0f,
                0.0f,  0.0f, 1.0f,

                -1.0f,  0.0f,  0.0f,
                -1.0f,  0.0f,  0.0f,
                -1.0f,  0.0f,  0.0f,
                -1.0f,  0.0f,  0.0f,
                -1.0f,  0.0f,  0.0f,
                -1.0f,  0.0f,  0.0f,

                1.0f,  0.0f,  0.0f,
                1.0f,  0.0f,  0.0f,
                1.0f,  0.0f,  0.0f,
                1.0f,  0.0f,  0.0f,
                1.0f,  0.0f,  0.0f,
                1.0f,  0.0f,  0.0f,

                0.0f, -1.0f,  0.0f,
                0.0f, -1.0f,  0.0f,
                0.0f, -1.0f,  0.0f,
                0.0f, -1.0f,  0.0f,
                0.0f, -1.0f,  0.0f,
                0.0f, -1.0f,  0.0f,

                0.0f,  1.0f,  0.0f,
                0.0f,  1.0f,  0.0f,
                0.0f,  1.0f,  0.0f,
                0.0f,  1.0f,  0.0f,
                0.0f,  1.0f,  0.0f,
                0.0f,  1.0f,  0.0f
        };

        float[] colours = new float[(int) (vertices.length / 0.75f)];
        Arrays.fill(colours, 1f);

        return loader.loadToVAO(vertices, normals, colours, textures, null, GL33C.GL_TRIANGLES);
    }

    public void disposeLoader() {
        loader.dispose();
    }

}

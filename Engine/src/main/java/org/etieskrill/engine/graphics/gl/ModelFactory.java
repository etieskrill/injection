package org.etieskrill.engine.graphics.gl;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.assimp.Vertex;
import org.etieskrill.engine.util.Loaders;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelFactory {

    public static Model.Builder rectangle(Vec2 position, Vec2 size) {
        return rectangle(position.getX(), position.getY(), size.getX(), size.getY(), null);
    }
    
    public static Model.Builder rectangle(float x, float y, float width, float height, Material material) {
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

        List<Vertex> vertices = new ArrayList<>();
        vertices.add(new Vertex(new Vec3(x, y, 0f), new Vec3(), new Vec2(0f)));
        vertices.add(new Vertex(new Vec3(x, y + height, 0f), new Vec3(), new Vec2(0f, 1f)));
        vertices.add(new Vertex(new Vec3(x + width, y, 0f), new Vec3(), new Vec2(1f, 0f)));
        vertices.add(new Vertex(new Vec3(x + width, y + height, 0f), new Vec3(), new Vec2(1f, 1f)));
        
        List<Short> indices = new ArrayList<>(List.of(new Short[]{0, 2, 1, 3, 1, 2}));
        
        Material mat = material != null ? material : Material.getBlank();
    
        Model.MemoryBuilder builder = new Model.MemoryBuilder("internal_model_factory:quad");
        builder
                .setMaterials(mat)
                .setMeshes(Mesh.Loader.loadToVAO(vertices, indices, mat));
        return builder;
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
    public Model circleSect(float x, float y, float radius, float start, float end, int segments) {
        
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
        
        //return loader.loadToVAO(vertices_a, colours, textures, indices_a, GL11C.GL_TRIANGLE_FAN);
        throw new UnsupportedOperationException("Currently under major reconstruction");
    }

    public Model circle(float x, float y, float radius, int segments) {
        //return circleSect(x, y, radius, 0, 360, segments);
        throw new UnsupportedOperationException("Currently under major reconstruction");
    }

    public Model roundedRect(float x, float y, float width, float height, float rounding, int segments) {
        if (rounding < 0) throw new IllegalArgumentException("corner rounding cannot be smaller than zero");
        if (2 * rounding > width || 2 * rounding > height)
            throw new IllegalArgumentException("rounding cannot exceed total width or height");

//        Model models = Model.of();

        float xTopLeft = x + rounding, yTopLeft = y + height - rounding;
        float xTopRight = x + width - rounding, yTopRight = y + height - rounding;
        float xBottomLeft = x + rounding, yBottomLeft = y + rounding;
        float xBottomRight = x + width - rounding, yBottomRight = y + rounding;

//        models.add(rectangle(x + rounding, y, width - 2 * rounding, height));
//        models.add(rectangle(x, y + rounding, rounding, height - 2 * rounding));
//        models.add(rectangle(x + width - rounding, y + rounding, rounding, height - 2 * rounding));
//        models.add(circleSect(xTopLeft, yTopLeft, rounding, 90, 180, segments));
//        models.add(circleSect(xTopRight, yTopRight, rounding, 0, 90, segments));
//        models.add(circleSect(xBottomLeft, yBottomLeft, rounding, 180, 270, segments));
//        models.add(circleSect(xBottomRight, yBottomRight, rounding, 270, 360, segments));

        throw new UnsupportedOperationException("Currently under major reconstruction");
    }

    public static Model box(Vec3 size) {
        Model box = Loaders.ModelLoader.get().load("internal_model_factory:cube", () -> Model.ofFile("cube.obj"));
        box.getTransform().setInitialScale(size);
        return box;
    }

    //TODO probs fibonacci or subdivision, test with phong, gouraud and flat shading
    public Model sphere(float radius, int subdivisions) {
        throw new UnsupportedOperationException("Currently under major reconstruction");
    }

}

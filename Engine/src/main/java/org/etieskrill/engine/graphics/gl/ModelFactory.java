package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.graphics.model.Vertex;
import org.etieskrill.engine.graphics.model.loader.MeshLoader;
import org.etieskrill.engine.util.Loaders;
import org.joml.*;
import org.lwjgl.BufferUtils;

import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModelFactory {

    public static Model.Builder rectangle(Vector2f position, Vector2f size) {
        return rectangle(position.x(), position.y(), size.x(), size.y(), null);
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
        vertices.add(new Vertex(new Vector3f(x, y, 0f), new Vector3f(), new Vector2f(0f)));
        vertices.add(new Vertex(new Vector3f(x, y + height, 0f), new Vector3f(), new Vector2f(0f, 1f)));
        vertices.add(new Vertex(new Vector3f(x + width, y, 0f), new Vector3f(), new Vector2f(1f, 0f)));
        vertices.add(new Vertex(new Vector3f(x + width, y + height, 0f), new Vector3f(), new Vector2f(1f, 1f)));

        List<Integer> indices = new ArrayList<>(List.of(new Integer[]{0, 2, 1, 3, 1, 2}));
        
        Material mat = material != null ? material : Material.getBlank();
    
        Model.MemoryBuilder builder = new Model.MemoryBuilder("internal_model_factory:quad");
        builder
                .setMaterials(mat)
                .addNodes(new Node("root", null, new Matrix4f(), Collections.singletonList(MeshLoader.loadToVAO(vertices, indices, mat))));
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

    public static Model box(Vector3f size) {
        Model baseBox = Loaders.ModelLoader.get().load("internal-model-factory:box", () -> Model.ofFile("box.obj"));
        Model box = new Model(baseBox);
        box.getInitialTransform().setScale(size);
        return box;
    }

    public static Model quadBox(Vector3f size) {
        Model quadBox = Loaders.ModelLoader.get().load("internal-model-loader:quad-box", () -> new Model.Builder("quad-box.obj").build());
        quadBox.getInitialTransform().setScale(size);
        return quadBox;
    }

    //TODO probs fibonacci or subdivision, test with phong, gouraud and flat shading
    public Model sphere(float radius, int subdivisions) {
        throw new UnsupportedOperationException("Currently under major reconstruction");
    }

}

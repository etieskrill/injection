package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.graphics.gl.VertexArrayObject;
import org.etieskrill.engine.graphics.model.*;
import org.joml.primitives.AABBf;

import java.util.List;

import static org.lwjgl.opengl.GL30C.glBindVertexArray;

public final class MeshLoader {
    //TODO builder
    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material) {
        return loadToVAO(vertices, indices, material, null, null, null);
    }

    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, AABBf boundingBox) {
        return loadToVAO(vertices, indices, material, null, boundingBox, null);
    }

    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, Mesh.DrawMode drawMode) {
        return loadToVAO(vertices, indices, material, null, null, drawMode);
    }

    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, List<Bone> bones, AABBf boundingBox, Mesh.DrawMode drawMode) {
        VertexArrayObject<Vertex> vao = VertexArrayObject
                .builder(VertexAccessor.getInstance())
                .vertexElements(vertices)
                .indices(indices)
                .build();
        unbindVAO();

        return new Mesh(material, bones != null ? bones : List.of(),
                vao, indices.size(), boundingBox,
                drawMode != null ? drawMode : Mesh.DrawMode.TRIANGLES
        );
    }

    private static void unbindVAO() {
        glBindVertexArray(0);
    }

}

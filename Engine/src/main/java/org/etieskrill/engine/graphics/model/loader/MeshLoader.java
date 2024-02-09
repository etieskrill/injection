package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Vertex;

import java.util.List;

import static org.etieskrill.engine.graphics.model.Mesh.GL_FLOAT_BYTE_SIZE;
import static org.etieskrill.engine.graphics.model.Vertex.*;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public final class MeshLoader {
    //TODO builder
    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material) {
        return loadToVAO(vertices, indices, material, null, null, null);
    }

    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, AABB boundingBox) {
        return loadToVAO(vertices, indices, material, null, boundingBox, null);
    }

    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, Mesh.DrawMode drawMode) {
        return loadToVAO(vertices, indices, material, null, null, drawMode);
    }

    public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, List<Bone> bones, AABB boundingBox, Mesh.DrawMode drawMode) {
        int vao = createVAO();

        List<Float> _data = vertices.stream()
                .map(Vertex::toList)
                .flatMap(List::stream)
                .toList();
        float[] data = new float[_data.size()];
        for (int i = 0; i < _data.size(); i++) data[i] = _data.get(i);
        int vbo = prepareVBO(data);

        int[] _indices = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) _indices[i] = indices.get(i);
        int ebo = prepareIndexBuffer(_indices);

        unbindVAO();
        return new Mesh(material, bones != null ? bones : List.of(),
                vao, indices.size(), vbo, ebo, boundingBox,
                drawMode != null ? drawMode : Mesh.DrawMode.TRIANGLES
        );
    }

    private static int createVAO() {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        return vao;
    }

    private static int prepareVBO(float[] data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_READ);

        setVertexAttributePointer(0, POSITION_COMPONENTS, false, 0);
        setVertexAttributePointer(1, NORMAL_COMPONENTS, true,
                POSITION_COMPONENTS * GL_FLOAT_BYTE_SIZE);
        setVertexAttributePointer(2, TEXTURE_COMPONENTS, false,
                (POSITION_COMPONENTS + NORMAL_COMPONENTS) * GL_FLOAT_BYTE_SIZE);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    private static void setVertexAttributePointer(int index, int numComponents, boolean normalised, int offset) {
        glEnableVertexAttribArray(index);
        int totalStride = COMPONENTS * GL_FLOAT_BYTE_SIZE;
        glVertexAttribPointer(index, numComponents, GL_FLOAT, normalised, totalStride, offset);
    }

    private static int prepareIndexBuffer(int[] indices) {
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
        return ebo;
    }

    private static void unbindVAO() {
        glBindVertexArray(0);
    }
}

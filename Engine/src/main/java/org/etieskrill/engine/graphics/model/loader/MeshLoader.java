package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Vertex;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.List;

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

        ByteBuffer data = BufferUtils.createByteBuffer(vertices.size() * COMPONENT_BYTES);
        vertices.stream()
                .map(Vertex::block)
                .forEach(data::put);
        data.rewind();
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

    private static int prepareVBO(ByteBuffer data) {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_READ);

        setFloatPointer(0, POSITION_COMPONENTS, false, 0);
        setFloatPointer(1, NORMAL_COMPONENTS, true, POSITION_BYTES);
        setFloatPointer(2, TEXTURE_COMPONENTS, false, POSITION_BYTES + NORMAL_BYTES);
        setIntegerPointer(3, BONE_COMPONENTS, false, POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES);
        setFloatPointer(4, BONE_WEIGHT_COMPONENTS, false, POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES + BONE_BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    private static void setFloatPointer(int index, int numComponents, boolean normalised, int offset) {
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, numComponents, GL_FLOAT, normalised, COMPONENT_BYTES, offset);
    }

    private static void setIntegerPointer(int index, int numComponents, boolean normalised, int offset) {
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, numComponents, GL_INT, normalised, COMPONENT_BYTES, offset);
    }

    private static int prepareIndexBuffer(int[] indices) {
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_READ);
        return ebo;
    }

    private static void unbindVAO() {
        glBindVertexArray(0);
    }

}

package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.component.AABB;
import org.etieskrill.engine.graphics.gl.BufferObject;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Vertex;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.etieskrill.engine.graphics.gl.BufferObject.AccessType.READ;
import static org.etieskrill.engine.graphics.gl.BufferObject.Target.ELEMENT_ARRAY;
import static org.etieskrill.engine.graphics.model.Vertex.*;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL15C.GL_INT;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.*;

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
        int vao = createVAO(); //TODO use VertexArrayObject

        ByteBuffer data = BufferUtils.createByteBuffer(vertices.size() * COMPONENT_BYTES);
        vertices.forEach(vertex -> vertex.buffer(data));
        BufferObject vbo = prepareVBO(data);

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.size());
        indices.forEach(indexBuffer::put);
        BufferObject ebo = prepareIndexBuffer(indexBuffer);

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

    private static BufferObject prepareVBO(ByteBuffer data) {
        BufferObject vbo = BufferObject.create(data).accessType(READ).build();

        setFloatPointer(0, POSITION_COMPONENTS, false, 0);
        setFloatPointer(1, NORMAL_COMPONENTS, true, POSITION_BYTES);
        setFloatPointer(2, TEXTURE_COMPONENTS, false, POSITION_BYTES + NORMAL_BYTES);
        setFloatPointer(3, TANGENT_COMPONENTS, true, POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES);
        setFloatPointer(4, BITANGENT_COMPONENTS, true, POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES + TANGENT_BYTES);
        setIntegerPointer(5, BONE_COMPONENTS, POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES + TANGENT_BYTES + BITANGENT_BYTES);
        setFloatPointer(6, BONE_WEIGHT_COMPONENTS, false, POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES + TANGENT_BYTES + BITANGENT_BYTES + BONE_BYTES);

        GLUtils.checkErrorThrowing("Failed to setup vertex data");
        return vbo;
    }

    private static void setFloatPointer(int index, int numComponents, boolean normalised, int offset) {
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, numComponents, GL_FLOAT, normalised, COMPONENT_BYTES, offset);
    }

    private static void setIntegerPointer(int index, int numComponents, int offset) {
        glEnableVertexAttribArray(index);
        glVertexAttribIPointer(index, numComponents, GL_INT, COMPONENT_BYTES, offset); //TODO FUUUCKKCKKCKKC DFHTSIISTHTIISSS WHY DOES IT NOT WORK WITH A NON TYPED ATTRIB POINTER???___!__!!_!!1111 fufuuuuuuuUUUUUUUUUUUKKKK
    }

    private static BufferObject prepareIndexBuffer(IntBuffer buffer) {
        return BufferObject
                .create(buffer)
                .target(ELEMENT_ARRAY)
                .accessType(READ)
                .build();
    }

    private static void unbindVAO() {
        glBindVertexArray(0);
    }

}

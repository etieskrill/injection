package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.jocl.Sizeof;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.List;

public class Loader {

    public static final int GL_FLOAT_BYTE_SIZE = 4;

    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    private final List<Integer> ebos = new ArrayList<>();
    
    public RawModel loadToVAO(final float[] vertices, final short[] indices, final int drawMode) {
        int vao = createVAO();
        int vbo = storeInAttributeList(vertices, new float[(int) (vertices.length / 0.75)]);
        int ebo = bindIndicesBuffer(indices);
        unbindVAO();
        return new RawModel(vertices, indices, vao, vbo, ebo, indices.length, drawMode);
    }
    
    private int createVAO() {
        int vao = GL30C.glGenVertexArrays();
        vaos.add(vao);
        GL30C.glBindVertexArray(vao);
        return vao;
    }
    
    private void unbindVAO() {
        GL30C.glBindVertexArray(0);
    }
    
    private int storeInAttributeList(float[] vertices, float[] colours) {
        int vbo = GL15C.glGenBuffers();
        vbos.add(vbo);
        float[] data = FloatArrayMerger.merge(vertices, colours, 3, 4);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data, GL15C.GL_DYNAMIC_DRAW);
        GL20C.glVertexAttribPointer(0, 3, GL11C.GL_FLOAT, false, 7 * GL_FLOAT_BYTE_SIZE, 0);
        GL20C.glEnableVertexAttribArray(0);
        //GL33C.glVertexAttribPointer(1, 4, GL33C.GL_FLOAT, false, 7 * GL_FLOAT_BYTE_SIZE, 3 * GL_FLOAT_BYTE_SIZE);
        //GL33C.glEnableVertexAttribArray(1);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        return vbo;
    }
    
    private int bindIndicesBuffer(short[] indices) {
        int ebo = GL15C.glGenBuffers();
        ebos.add(ebo);
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices, GL15C.GL_DYNAMIC_DRAW);
        return ebo;
    }
    
    public void cleanup() {
        for (int vao : vaos) GL15C.glDeleteBuffers(vao);
        for (int vbo : vbos) GL30C.glDeleteVertexArrays(vbo);
        for (int ebo : ebos) GL15C.glDeleteBuffers(ebo);
    }
    
}

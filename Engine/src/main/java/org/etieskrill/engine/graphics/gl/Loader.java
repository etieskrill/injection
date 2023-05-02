package org.etieskrill.engine.graphics.gl;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.List;

public class Loader {
    
    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    private final List<Integer> ebos = new ArrayList<>();
    
    public static int eboTemp = 0;
    
    public RawMemoryModel loadToVAO(final float[] vertices, final int[] indices, final int drawMode) {
        int vao = createVAO();
        storeInAttributeList(0, vertices);
        bindIndicesBuffer(indices);
        unbindVAO();
        return new RawMemoryModel(vao, indices.length, drawMode);
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
    
    private void storeInAttributeList(int index, float[] vertices) {
        int vbo = GL15C.glGenBuffers();
        vbos.add(vbo);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);
        GL20C.glVertexAttribPointer(index, 2, GL11C.GL_FLOAT, false, 0, 0);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
    }
    
    private void bindIndicesBuffer(int[] indices) {
        int ebo = GL15C.glGenBuffers();
        eboTemp = ebo;
        ebos.add(ebo);
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices, GL15C.GL_STATIC_DRAW);
        //glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    public void cleanup() {
        for (int vao : vaos) GL15C.glDeleteBuffers(vao);
        for (int vbo : vbos) GL30C.glDeleteVertexArrays(vbo);
        for (int ebo : ebos) GL15C.glDeleteBuffers(ebo);
    }
    
}

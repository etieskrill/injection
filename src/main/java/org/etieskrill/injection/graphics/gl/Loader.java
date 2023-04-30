package org.etieskrill.injection.graphics.gl;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL30C.*;

public class Loader {
    
    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    private final List<Integer> ebos = new ArrayList<>();
    
    public static int eboTemp = 0;
    
    public RawMemoryModel loadToVAO(float[] vertices, int[] indices) {
        int vao = createVAO();
        storeInAttributeList(0, vertices);
        bindIndicesBuffer(indices);
        unbindVAO();
        return new RawMemoryModel(vao, indices.length);
    }
    
    private int createVAO() {
        int vao = glGenVertexArrays();
        vaos.add(vao);
        glBindVertexArray(vao);
        return vao;
    }
    
    private void unbindVAO() {
        glBindVertexArray(0);
    }
    
    private void storeInAttributeList(int index, float[] vertices) {
        int vbo = glGenBuffers();
        vbos.add(vbo);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glVertexAttribPointer(index, 2, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private void bindIndicesBuffer(int[] indices) {
        int ebo = glGenBuffers();
        eboTemp = ebo;
        ebos.add(ebo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        //glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    public void cleanup() {
        for (int vao : vaos) glDeleteBuffers(vao);
        for (int vbo : vbos) glDeleteVertexArrays(vbo);
        for (int ebo : ebos) glDeleteBuffers(ebo);
    }
    
}

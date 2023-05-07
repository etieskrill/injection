package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.lwjgl.opengl.GL33C;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.graphics.gl.RawModel.*;

public class Loader {

    public static final int GL_FLOAT_BYTE_SIZE = Float.BYTES;

    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    private final List<Integer> ebos = new ArrayList<>();
    
    public RawModel loadToVAO(float[] vertices, float[] colours, float[] textures, short[] indices, int drawMode) {
        int vao = createVAO();
        int vbo = storeInAttributeList(vertices, colours, textures);
        int ebo = bindIndicesBuffer(indices);
        unbindVAO();
        return new RawModel(vertices, colours, textures, indices, vao, vbo, ebo, indices.length, drawMode);
    }
    
    private int createVAO() {
        int vao = GL33C.glGenVertexArrays();
        vaos.add(vao);

        GL33C.glBindVertexArray(vao);
        
        return vao;
    }
    
    private void unbindVAO() {
        GL33C.glBindVertexArray(0);
    }
    
    private int storeInAttributeList(float[] vertices, float[] colours, float[] textures) {
        int vbo = GL33C.glGenBuffers();
        vbos.add(vbo);

        int vertexLength = vertices.length / MODEL_POSITION_COMPONENTS;
        if (vertexLength != colours.length / MODEL_COLOUR_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex colours");
        else if (vertexLength != textures.length / MODEL_TEXTURE_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex textures");

        float[] data = FloatArrayMerger.merge(vertices, colours, MODEL_POSITION_COMPONENTS, MODEL_COLOUR_COMPONENTS);
        data = FloatArrayMerger.merge(data, textures, MODEL_POSITION_COMPONENTS + MODEL_COLOUR_COMPONENTS,
                MODEL_TEXTURE_COMPONENTS);
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, data, GL33C.GL_DYNAMIC_DRAW);

        int totalStride = MODEL_POSITION_COMPONENTS + MODEL_COLOUR_COMPONENTS + MODEL_TEXTURE_COMPONENTS;
        
        GL33C.glVertexAttribPointer(0, 3, GL33C.GL_FLOAT, false,
                totalStride * GL_FLOAT_BYTE_SIZE, 0);
        GL33C.glEnableVertexAttribArray(0);
        GL33C.glVertexAttribPointer(1, 4, GL33C.GL_FLOAT, false,
                totalStride * GL_FLOAT_BYTE_SIZE, MODEL_POSITION_COMPONENTS * GL_FLOAT_BYTE_SIZE);
        GL33C.glEnableVertexAttribArray(1);
        GL33C.glVertexAttribPointer(2, 2, GL33C.GL_FLOAT, false,
                totalStride * GL_FLOAT_BYTE_SIZE, (MODEL_POSITION_COMPONENTS + MODEL_COLOUR_COMPONENTS) * GL_FLOAT_BYTE_SIZE);
        GL33C.glEnableVertexAttribArray(2);

        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, 0);
        return vbo;
    }
    
    private int bindIndicesBuffer(short[] indices) {
        int ebo = GL33C.glGenBuffers();
        ebos.add(ebo);

        GL33C.glBindBuffer(GL33C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL33C.glBufferData(GL33C.GL_ELEMENT_ARRAY_BUFFER, indices, GL33C.GL_DYNAMIC_DRAW);
        
        return ebo;
    }
    
    public void cleanup() {
        for (int vao : vaos) GL33C.glDeleteBuffers(vao);
        for (int vbo : vbos) GL33C.glDeleteVertexArrays(vbo);
        for (int ebo : ebos) GL33C.glDeleteBuffers(ebo);
    }
    
}

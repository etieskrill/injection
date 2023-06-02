package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.lwjgl.opengl.GL33C;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.graphics.gl.RawModel.*;
import static org.lwjgl.opengl.GL33C.*;

public class Loader {

    public static final int GL_FLOAT_BYTE_SIZE = Float.BYTES;

    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    private final List<Integer> ebos = new ArrayList<>();
    
    public RawModel loadToVAO(float[] vertices, float[] colours, float[] textures, short[] indices, int drawMode) {
        boolean hasIndexBuffer = indices != null;

        int vao = createVAO();
        storeInAttributeList(vertices, new float[vertices.length * MODEL_NORMAL_COMPONENTS / MODEL_POSITION_COMPONENTS], colours, textures);
        if (hasIndexBuffer) bindIndicesBuffer(indices);
        unbindVAO();

        return new RawModel(vao, hasIndexBuffer ? indices.length : vertices.length, drawMode, hasIndexBuffer);
    }

    public RawModel loadToVAO(float[] vertices, float[] normals, float[] colours, float[] textures, short[] indices, int drawMode) {
        boolean hasIndexBuffer = indices != null;

        int vao = createVAO();
        storeInAttributeList(vertices, normals, colours, textures);
        if (hasIndexBuffer) bindIndicesBuffer(indices);
        unbindVAO();

        return new RawModel(vao, hasIndexBuffer ? indices.length : vertices.length, drawMode, hasIndexBuffer);
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
    
    private int storeInAttributeList(float[] vertices, float[] normals, float[] colours, float[] textures) {
        int vbo = glGenBuffers();
        vbos.add(vbo);

        int vertexLength = vertices.length / MODEL_POSITION_COMPONENTS;
        if (vertexLength != normals.length / MODEL_NORMAL_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex normals");
        else if (vertexLength != colours.length / MODEL_COLOUR_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex colours");
        else if (vertexLength != textures.length / MODEL_TEXTURE_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex textures");

        float[] data = FloatArrayMerger.merge(vertices, normals, MODEL_POSITION_COMPONENTS, MODEL_NORMAL_COMPONENTS);
        data = FloatArrayMerger.merge(data, colours, MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS, MODEL_COLOUR_COMPONENTS);
        data = FloatArrayMerger.merge(data, textures, MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS + MODEL_COLOUR_COMPONENTS,
                MODEL_TEXTURE_COMPONENTS);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);

        int totalStride = (MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS + MODEL_COLOUR_COMPONENTS + MODEL_TEXTURE_COMPONENTS) * GL_FLOAT_BYTE_SIZE;
        
        glVertexAttribPointer(0, MODEL_POSITION_COMPONENTS, GL_FLOAT, false,
                totalStride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, MODEL_NORMAL_COMPONENTS, GL_FLOAT, true,
                totalStride, MODEL_POSITION_COMPONENTS * GL_FLOAT_BYTE_SIZE);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, MODEL_COLOUR_COMPONENTS, GL_FLOAT, false,
                totalStride, (MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS) * GL_FLOAT_BYTE_SIZE);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, MODEL_TEXTURE_COMPONENTS, GL_FLOAT, false,
                totalStride, (MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS + MODEL_COLOUR_COMPONENTS) * GL_FLOAT_BYTE_SIZE);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }
    
    private int bindIndicesBuffer(short[] indices) {
        int ebo = glGenBuffers();
        ebos.add(ebo);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
        
        return ebo;
    }
    
    public void dispose() {
        for (int vao : vaos) glDeleteBuffers(vao);
        for (int vbo : vbos) glDeleteVertexArrays(vbo);
        for (int ebo : ebos) glDeleteBuffers(ebo);
    }
    
}

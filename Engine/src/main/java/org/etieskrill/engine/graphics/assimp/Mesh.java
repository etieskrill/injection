package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.Disposable;

import java.util.List;

import static org.etieskrill.engine.graphics.assimp.Vertex.*;
import static org.etieskrill.engine.graphics.assimp.Vertex.COMPONENTS;
import static org.lwjgl.opengl.GL33C.*;

public class Mesh implements Disposable {
    
    public static final int GL_FLOAT_BYTE_SIZE = Float.BYTES;
    
    private final Material material;
    private final int vao, numIndices, vbo, ebo;
    
    public static final class Loader {
        public static Mesh loadToVAO(List<Vertex> vertices, List<Short> indices, Material material) {
            int vao = createVAO();
    
            if (indices.isEmpty()) System.out.println(vertices.size());
        
            List<Float> _data = vertices.stream()
                    .map(Vertex::toList)
                    .flatMap(List::stream)
                    .toList();
            float[] data = new float[_data.size()];
            for (int i = 0; i < _data.size(); i++) data[i] = _data.get(i);
            int vbo = prepareVBO(data);
        
            short[] _indices = new short[indices.size()];
            for (int i = 0; i < indices.size(); i++) _indices[i] = indices.get(i);
            int ebo = prepareIndexBuffer(_indices);
        
            unbindVAO();
            return new Mesh(material, vao, indices.size(), vbo, ebo);
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
    
        private static int prepareIndexBuffer(short[] indices) {
            int ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
            return ebo;
        }
    
        private static void unbindVAO() {
            glBindVertexArray(0);
        }
    }
    
    public Mesh(Material material,
                int vao, int numIndices, int vbo, int ebo) {
        this.material = material;
        
        this.vao = vao;
        this.numIndices = numIndices;
        this.vbo = vbo;
        this.ebo = ebo;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public int getNumIndices() {
        return numIndices;
    }
    
    public int getVao() {
        return vao;
    }
    
    @Override
    public void dispose() {
        material.dispose();
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
    
}

package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.lwjgl.opengl.GL33C;

public class RawModel {

    public static final int
            MODEL_POSITION_COMPONENTS = 3,
            MODEL_COLOUR_COMPONENTS = 4,
            MODEL_TEXTURE_COMPONENTS = 2;
    
    public enum ModelComponents {
        MODEL_POSITION_COMPONENTS(3),
        MODEL_COLOUR_COMPONENTS(4),
        MODEL_TEXTURE_COMPONENTS(2);
    
        private final int components;
        
        ModelComponents(int numComponents) {
            this.components = numComponents;
        }
    
        public int getComponents() {
            return components;
        }
    }
    
    private final float[] vertices;
    private final float[] colours;
    private final float[] textures;
    private final short[] indices;
    
    private final int vao, vbo, ebo;
    private final int numVertices;
    private int drawMode;
    
    public RawModel(float[] vertices, float[] colours, float[] textures, short[] indices,
                    int vao, int vbo, int ebo,
                    int numVertices, int drawMode) {
        this.vertices = vertices;
        this.colours = colours;
        this.textures = textures;
        this.indices = indices;
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
        this.numVertices = numVertices;
        this.drawMode = drawMode;
    }
    
    public RawModel(RawModel rawModel) {
        this(
                rawModel.getVertices(), rawModel.getColours(), rawModel.getTextures(), rawModel.getIndices(),
                rawModel.getVao(), rawModel.getVbo(), rawModel.getEbo(),
                rawModel.getNumVertices(), rawModel.getDrawMode()
        );
    }
    
    /*public static RawModel get(int vao, int vbo, int ebo) {
        return new RawModelWrapper(vao, vbo, ebo);
    }
    
    private static class RawModelWrapper extends RawModel {
        public RawModelWrapper(int vao, int vbo, int ebo) {
            super(vao, vbo, ebo);
        }
    
        @Override
        public void bind() {}
    
        @Override
        public void unbind() {}
    }*/
    
    public void bind() {};
    public void unbind() {};
    
    //public void update() {};
    
    public void update(float[] vertices, float[] colours, float[] textures, short[] indices, int drawMode) {
        GL33C.glBindVertexArray(this.vao);
        float[] data = FloatArrayMerger.merge(vertices, colours, MODEL_POSITION_COMPONENTS, MODEL_COLOUR_COMPONENTS);
        data = FloatArrayMerger.merge(data, textures, MODEL_POSITION_COMPONENTS + MODEL_COLOUR_COMPONENTS,
                MODEL_TEXTURE_COMPONENTS);
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, data, GL33C.GL_DYNAMIC_DRAW);
        GL33C.glBindBuffer(GL33C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL33C.glBufferData(GL33C.GL_ELEMENT_ARRAY_BUFFER, indices, GL33C.GL_DYNAMIC_DRAW);
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, 0);
        this.drawMode = drawMode;
    }
    
    public float[] getVertices() {
        return vertices;
    }
    
    public float[] getColours() {
        return colours;
    }

    public float[] getTextures() {
        return textures;
    }

    public short[] getIndices() {
        return indices;
    }
    
    public int getVao() {
        return vao;
    }
    
    public int getVbo() {
        return vbo;
    }
    
    public int getEbo() {
        return ebo;
    }
    
    public int getNumVertices() {
        return numVertices;
    }

    public int getDrawMode() {
        return drawMode;
    }
    
}

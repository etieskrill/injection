package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;

public class RawModel {

    public static final int
            MODEL_POSITION_COMPONENTS = 3,
            MODEL_COLOUR_COMPONENTS = 4,
            MODEL_TEXTURE_COMPONENTS = 2;
    
    private final float[] vertices;
    private final float[] colours;
    private final float[] textures;
    private final short[] indices;
    
    private final int vao, vbo, ebo;
    private final int numVertices;
    private int drawMode;
    
    public RawModel(float[] vertices, float[] colours, float[] textures, short[] indices, int vao, int vbo, int ebo, int numVertices, int drawMode) {
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
        this.vertices = rawModel.getVertices();
        this.colours = rawModel.getColours();
        this.textures = rawModel.getTextures();
        this.indices = rawModel.getIndices();
        this.vao = rawModel.getVao();
        this.vbo = rawModel.getVbo();
        this.ebo = rawModel.getEbo();
        this.numVertices = rawModel.getNumVertices();
        this.drawMode = rawModel.getDrawMode();
    }
    
    public void update(float[] vertices, float[] colours, float[] textures, short[] indices, int drawMode) {
        GL30C.glBindVertexArray(this.vao);
        float[] data = FloatArrayMerger.merge(vertices, colours, MODEL_POSITION_COMPONENTS, MODEL_COLOUR_COMPONENTS);
        data = FloatArrayMerger.merge(data, textures, MODEL_POSITION_COMPONENTS + MODEL_COLOUR_COMPONENTS,
                MODEL_TEXTURE_COMPONENTS);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
        GL30C.glBufferData(GL15C.GL_ARRAY_BUFFER, data, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL30C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
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

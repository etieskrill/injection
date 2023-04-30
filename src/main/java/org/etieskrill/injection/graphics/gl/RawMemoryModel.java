package org.etieskrill.injection.graphics.gl;

public class RawMemoryModel {
    
    private final int vao;
    private final int numVertices;
    
    public RawMemoryModel(int vao, int numVertices) {
        this.vao = vao;
        this.numVertices = numVertices;
    }
    
    public int getVao() {
        return vao;
    }
    
    public int getNumVertices() {
        return numVertices;
    }
    
}

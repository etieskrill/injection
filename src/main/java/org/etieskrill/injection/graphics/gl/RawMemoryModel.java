package org.etieskrill.injection.graphics.gl;

public class RawMemoryModel {
    
    private final int vao;
    private final int numVertices;
    private final int drawMode;
    
    public RawMemoryModel(final int vao, final int numVertices, final int drawMode) {
        this.vao = vao;
        this.numVertices = numVertices;
        this.drawMode = drawMode;
    }
    
    public int getVao() {
        return vao;
    }
    
    public int getNumVertices() {
        return numVertices;
    }

    public int getDrawMode() {
        return drawMode;
    }
}

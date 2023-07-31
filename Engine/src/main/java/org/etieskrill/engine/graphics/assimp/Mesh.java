package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.graphics.gl.Texture;

import java.util.Vector;

public class Mesh {
    
    private final Vector<Vertex> vertices;
    private final Vector<Short> indices;
    private final Material material;
    
    private final int vao, vbo, ebo;
    
    public Mesh(Vector<Vertex> vertices, Vector<Short> indices, Material material,
                int vao, int vbo, int ebo) {
        this.vertices = vertices;
        this.indices = indices;
        this.material = material;
        
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public Vector<Short> getIndices() {
        return indices;
    }
    
    public int getVao() {
        return vao;
    }
    
}

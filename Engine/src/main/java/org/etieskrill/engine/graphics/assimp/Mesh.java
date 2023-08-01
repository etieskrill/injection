package org.etieskrill.engine.graphics.assimp;

import glm.mat._4.Mat4;
import org.etieskrill.engine.graphics.gl.Texture;

import java.util.Vector;

public class Mesh {
    
    private final Vector<Vertex> vertices;
    private final Vector<Short> indices;
    private final Material material;
    
    private final int vao, vbo, ebo;
    
    private final Mat4 transform;
    
    public Mesh(Vector<Vertex> vertices, Vector<Short> indices, Material material,
                int vao, int vbo, int ebo,
                Mat4 transform) {
        this.vertices = vertices;
        this.indices = indices;
        this.material = material;
        
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
        
        this.transform = transform;
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

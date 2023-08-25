package org.etieskrill.engine.graphics.assimp;

import glm.mat._4.Mat4;

public class Mesh {
    
    private final Material material;
    private final int vao, numIndices, vbo, ebo;
    private final Mat4 transform;
    
    public Mesh(Material material,
                int vao, int numIndices, int vbo, int ebo,
                Mat4 transform) {
        this.material = material;
        
        this.vao = vao;
        this.numIndices = numIndices;
        this.vbo = vbo;
        this.ebo = ebo;
        
        this.transform = transform;
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
    
    public Mat4 getTransform() {
        return transform;
    }
    
}

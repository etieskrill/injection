package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.graphics.gl.Texture;

import java.util.Vector;

public class Material {
    
    //TODO number max gl texture units check
    
    private final Vector<Texture> textures;
    
    public Material() {
        this.textures = new Vector<>();
    }
    
    public Material(Vector<Texture> textures) {
        this.textures = new Vector<>(textures);
    }
    
    public Vector<Texture> getTextures() {
        return textures;
    }
    
    public void addTexture(Texture texture) {
        this.textures.add(texture);
    }
    
}

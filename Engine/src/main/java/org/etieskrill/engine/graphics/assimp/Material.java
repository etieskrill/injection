package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.Texture;

import java.util.List;
import java.util.Vector;

//TODO separate phong/pbr via inheritance
public class Material implements Disposable {
    
    //TODO number max gl texture units check
    private final Vector<Texture> textures;
    
    private final float shininess, shininessStrength;
    
    public static final class Builder {
        private Vector<Texture> textures = new Vector<>();
        private float shininess = 32f, shininessStrength = 1f;
        
        public Builder addTextures(Texture... textures) {
            this.textures.addAll(List.of(textures));
            return this;
        }
        
        public Builder addTextures(Vector<Texture> textures) {
            this.textures.addAll(textures);
            return this;
        }
    
        public void setShininess(float shininess) {
            this.shininess = shininess;
        }
    
        public void setShininessStrength(float shininessStrength) {
            this.shininessStrength = shininessStrength;
        }
    
        public Material build() {
            return new Material(textures, shininess, shininessStrength);
        }
    }
    
    public static Material getBlank() {
        return new Material.Builder().build();
    }
    
    private Material(Vector<Texture> textures, float shininess, float shininessStrength) {
        this.textures = new Vector<>(textures);
        this.shininess = shininess;
        this.shininessStrength = shininessStrength;
    }
    
    public Vector<Texture> getTextures() {
        return textures;
    }
    
    public float getShininess() {
        return shininess;
    }
    
    public float getShininessStrength() {
        return shininessStrength;
    }
    
    private boolean wasAlreadyDisposed = false;
    
    @Override
    public void dispose() {
        if (wasAlreadyDisposed) return;
        textures.forEach(Texture::dispose);
        wasAlreadyDisposed = true;
    }
    
}

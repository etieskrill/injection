package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;

import java.util.LinkedList;
import java.util.List;

//TODO separate phong/pbr via inheritance
public class Material implements Disposable {
    
    //TODO number max gl texture units check
    private final List<AbstractTexture> textures;
    
    private final float shininess, shininessStrength;
    
    public static final class Builder {
        private List<AbstractTexture> textures = new LinkedList<>();
        private float shininess = 32f, shininessStrength = 1f;
        
        public Builder addTextures(AbstractTexture... textures) {
            this.textures.addAll(List.of(textures));
            return this;
        }
        
        public Builder addTextures(List<AbstractTexture> textures) {
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
    
    private Material(List<AbstractTexture> textures, float shininess, float shininessStrength) {
        this.textures = new LinkedList<>(textures);
        this.shininess = shininess;
        this.shininessStrength = shininessStrength;
    }
    
    public List<AbstractTexture> getTextures() {
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
        textures.forEach(AbstractTexture::dispose);
        wasAlreadyDisposed = true;
    }
    
}

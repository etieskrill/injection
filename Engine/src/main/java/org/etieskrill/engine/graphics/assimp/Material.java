package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.graphics.gl.Texture;

import java.util.List;
import java.util.Vector;

public class Material {
    
    //TODO number max gl texture units check
    private final Vector<Texture> textures;
    
    private final float shininess;
    
    public static final class Builder {
        private Vector<Texture> textures = new Vector<>();
        private float shininess = 32f;
    
        public Builder setTextures(Texture... textures) {
            this.textures = new Vector<>(List.of(textures));
            return this;
        }
        
        public Builder setTextures(Vector<Texture> textures) {
            this.textures = textures;
            return this;
        }
    
        public Builder setShininess(float shininess) {
            this.shininess = shininess;
            return this;
        }
    
        public Material build() {
            return new Material(textures, shininess);
        }
    }
    
    public static Material getBlank() {
        return new Material.Builder().build();
    }
    
    private Material(Vector<Texture> textures, float shininess) {
        this.textures = new Vector<>(textures);
        this.shininess = shininess;
    }
    
    public Vector<Texture> getTextures() {
        return textures;
    }
    
    public void addTexture(Texture texture) {
        this.textures.add(texture);
    }
    
    public float getShininess() {
        return shininess;
    }
    
}

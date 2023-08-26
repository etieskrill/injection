package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.Texture;

import java.util.List;
import java.util.Vector;

public class Material implements Disposable {
    
    //TODO number max gl texture units check
    private final Vector<Texture> textures;
    
    public static final class Builder {
        private Vector<Texture> textures = new Vector<>();
    
        public Builder setTextures(Texture... textures) {
            this.textures = new Vector<>(List.of(textures));
            return this;
        }
        
        public Builder setTextures(Vector<Texture> textures) {
            this.textures = textures;
            return this;
        }
        
        public Material build() {
            return new Material(textures);
        }
    }
    
    //TODO add custom implementation for single shininess value (should be more efficient) ((in edge cases))
    
    public static Material getBlank() {
        return new Material.Builder().build();
    }
    
    private Material(Vector<Texture> textures) {
        this.textures = new Vector<>(textures);
    }
    
    public Vector<Texture> getTextures() {
        return textures;
    }
    
    public void addTexture(Texture texture) {
        this.textures.add(texture);
    }
    
    private boolean wasAlreadyDisposed = false;
    
    @Override
    public void dispose() {
        if (wasAlreadyDisposed) return;
        textures.forEach(Texture::dispose);
        wasAlreadyDisposed = true;
    }
    
}

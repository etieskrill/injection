package org.etieskrill.engine.graphics.gl;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

public class Model extends RawModel {
    
    public static final int GL_MAX_TEXTURE_UNITS = 16;
    
    private final Map<Integer, Texture> textures;
    
    public Model(RawModel rawModel) {
        super(rawModel);
        this.textures = new HashMap<>();
    }
    
    public Model(RawModel rawModel, Map<Integer, Texture> textures) {
        super(rawModel);
        if (textures == null) throw new IllegalArgumentException("texture list argument must not be null");
        if (textures.size() > GL_MAX_TEXTURE_UNITS)
            throw new IllegalArgumentException("cannot store more texture objects than the platform allows");
        this.textures = textures;
    }
    
    public Model addTexture(Texture texture, int unit) {
        if (textures.putIfAbsent(unit, texture) != null)
            throw new IllegalArgumentException("Cannot add texture to a unit that is already occupied");
        return this;
    }
    
    public Model replaceTexture(Texture texture, int unit) {
        if (!textures.containsKey(unit))
            throw new IllegalArgumentException("Cannot replace texture in an empty unit");
        textures.put(unit, texture);
        return this;
    }
    
    public void clearTextures() {
        textures.clear();
    }
    
    public void bind() {
        textures.forEach((unit, texture) -> texture.bind(unit));
    }
    
    public static void unbind() {
        for (int i = 0; i < GL_MAX_TEXTURE_UNITS; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }
    
}

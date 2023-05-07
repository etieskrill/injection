package org.etieskrill.engine.graphics.gl;

public class TexturedModel extends RawModel {
    
    private final Texture texture;
    
    public TexturedModel(RawModel rawModel, Texture texture) {
        super(rawModel);
        this.texture = texture;
    }
    
    @Override
    public void update(float[] vertices, float[] colours, float[] textures, short[] indices, int drawMode) {
        super.update(vertices, colours, textures, indices, drawMode);
    }
    
}

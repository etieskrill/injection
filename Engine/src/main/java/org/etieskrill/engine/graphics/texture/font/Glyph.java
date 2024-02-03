package org.etieskrill.engine.graphics.texture.font;

import org.joml.Vector2f;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;

public class Glyph {
    
    private final Vector2fc size;
    private final Vector2fc position;
    private final Vector2fc advance;
    private final Texture2D texture;
    
    private final Character character;
    
    public Glyph(Vector2fc size, Vector2fc position, Vector2fc advance, Texture2D texture, @Nullable Character character) {
        this.size = size;
        this.position = position;
        this.advance = advance;
        this.texture = texture;
        this.character = character;
    }
    
    public Vector2fc getSize() {
        return size;
    }
    
    public Vector2fc getPosition() {
        return position;
    }
    
    public Vector2fc getAdvance() {
        return advance;
    }
    
    public Texture2D getTexture() {
        return texture;
    }
    
    public @Nullable Character getCharacter() {
        return character;
    }
    
    @Override
    public String toString() {
        return "Glyph{" +
                "size=" + size +
                ", position=" + position +
                ", advance=" + advance +
                ", texture=" + texture +
                ", character=" + character +
                '}';
    }
    
}

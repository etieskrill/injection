package org.etieskrill.engine.graphics.texture.font;

import glm_.vec2.Vec2;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Nullable;

public class Glyph {
    
    private final Vec2 size;
    private final Vec2 position;
    private final Vec2 advance;
    private final Texture2D texture;
    
    private final Character character;
    
    public Glyph(Vec2 size, Vec2 position, Vec2 advance, Texture2D texture, @Nullable Character character) {
        this.size = size;
        this.position = position;
        this.advance = advance;
        this.texture = texture;
        this.character = character;
    }
    
    public Vec2 getSize() {
        return size;
    }
    
    public Vec2 getPosition() {
        return position;
    }
    
    public Vec2 getAdvance() {
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

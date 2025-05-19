package org.etieskrill.engine.graphics.text;

import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;

/**
 * A glyph represents a single unicode character belonging to a specific font, where the font family, style and size are
 * defined. This includes the metrics required in order to draw the glyph.
 * <p>
 * A glyph may have a singular corresponding texture by which it should be rendered. Most implementations however use
 * some form of a texture atlas in order to avoid switching textures for each glyph drawn, in which case the
 * {@link Glyph#texture} will be {@code null}.
 */
public class Glyph {
    
    private final Vector2fc size;
    private final Vector2fc position;
    private final Vector2fc advance;

    private final @Nullable Integer textureIndex;
    private final @Nullable Texture2D texture;

    private final @Nullable Character character;

    /**
     * @param size         the visible size of the glyph
     * @param position     offset from the origin point
     * @param advance      the amount of space this glyph actually takes in a line of text
     * @param textureIndex the index in the texture atlas, if any
     * @param character    the character this glyph represents, if any
     */
    public Glyph(Vector2fc size, Vector2fc position, Vector2fc advance,  @Nullable Integer textureIndex, @Nullable Character character) {
        this(size, position, advance, null, textureIndex, character);
    }

    /**
     * @param size         the visible size of the glyph
     * @param position     offset from the origin point
     * @param advance      the amount of space this glyph actually takes in a line of text
     * @param texture      the glyph's texture, if any
     * @param character    the character this glyph represents, if any
     */
    public Glyph(Vector2fc size, Vector2fc position, Vector2fc advance, @Nullable Texture2D texture,  @Nullable Character character) {
        this(size, position, advance, texture, null, character);
    }

    private Glyph(Vector2fc size,
                  Vector2fc position,
                  Vector2fc advance,
                  @Nullable Texture2D texture,
                  @Nullable Integer textureIndex,
                  @Nullable Character character) {
        this.size = size;
        this.position = position;
        this.advance = advance;
        this.texture = texture;
        this.textureIndex = textureIndex;
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

    public @Nullable Integer getTextureIndex() {
        return textureIndex;
    }

    public @Nullable Texture2D getTexture() {
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

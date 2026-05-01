package org.etieskrill.engine.graphics.text;

import org.etieskrill.engine.common.Disposable;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public interface Font extends Disposable {

    int NUM_CHARS_ASCII = 128;
    Vector2ic INVALID_PIXEL_SIZE = new Vector2i(-1);

    /**
     * Gets a character's glyph if it is present in the font's charset.
     * Returns the null glyph if the character could not be found in the font.
     *
     * @param c the character whose glyph to get
     * @return the character's glyph, else the null glyph
     */
    @NotNull Glyph getGlyph(char c);

    /**
     * Translates a {@code String} to its corresponding glyphs for rendering.
     * Characters whose glyphs are not present in the font are translated to the null glyph.
     *
     * @param s the string whose glyphs to get
     * @return the glyphs representing the string
     */
    @NotNull Glyph[] getGlyphs(String s);

    Vector2ic getPixelSize();
    int getLineHeight();
    int getMinLineHeight();

}

package org.etieskrill.engine.graphics.text

import org.etieskrill.engine.common.Disposable
import org.joml.Vector2i
import org.joml.Vector2ic

interface Font : Disposable {

    companion object {
        const val NUM_CHARS_ASCII = 128
        val INVALID_PIXEL_SIZE: Vector2ic = Vector2i(-1)
    }

    val pixelSize: Vector2ic
    val lineHeight: Int
    val minLineHeight: Int

    /**
     * Gets a character's glyph if it is present in the font's charset.
     * Returns the null glyph if the character could not be found in the font.
     *
     * @param c the character whose glyph to get
     * @return the character's glyph, else the null glyph
     */
    fun getGlyph(c: Char): Glyph

    /**
     * Translates a [String] to its corresponding glyphs for rendering.
     * Characters whose glyphs are not present in the font are translated to the null glyph.
     *
     * @param s the string whose glyphs to get
     * @return the glyphs representing the string
     */
    fun getGlyphs(s: String): Array<Glyph>

}

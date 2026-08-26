package org.etieskrill.engine.graphics.text

import org.etieskrill.engine.graphics.texture.ArrayTexture2D
import org.joml.Vector2ic

class BitmapFont(
    private val glyphs: Map<Char, Glyph>,

    override val lineHeight: Int,
    override val minLineHeight: Int,
    override val pixelSize: Vector2ic,

    val family: String,
    val style: String,

    val textures: ArrayTexture2D
) : Font {

    override fun getGlyph(c: Char): Glyph = glyphs[c] ?: glyphs[0.toChar()]!!

    override fun getGlyphs(s: String): Array<Glyph> = s.toCharArray().map { getGlyph(it) }.toTypedArray()

}

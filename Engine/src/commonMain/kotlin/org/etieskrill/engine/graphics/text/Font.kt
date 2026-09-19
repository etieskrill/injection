package org.etieskrill.engine.graphics.text

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.ResourceLoadException
import org.etieskrill.engine.util.EngineFontLoader
import org.etieskrill.engine.util.extension
import org.etieskrill.engine.util.path
import org.joml.Vector2i
import org.joml.Vector2ic

private val logger = KotlinLogging.logger {}

interface Font {

    companion object {
        const val NUM_CHARS_ASCII = 128
        val INVALID_PIXEL_SIZE: Vector2ic = Vector2i(-1)

        const val DEFAULT_FONT_SIZE = 24
        const val DEFAULT_FONT = "fonts/AGENCYB.TTF"

        fun getDefault(pixelHeight: Int = DEFAULT_FONT_SIZE, path: String = DEFAULT_FONT): Font {
            require(path.extension.lowercase() == "ttf") { "Must be TrueType file, but was ${path.extension}" }

            val generatorFont = EngineFontLoader.load("ttf:${path.path.lowercase()}:$pixelHeight") {
                try {
                    return@load TrueTypeFont(path)
                } catch (ex: ResourceLoadException) {
                    logger.warn(ex) { "Failed to load font" }
                    try {
                        return@load TrueTypeFont(DEFAULT_FONT)
                    } catch (ex: ResourceLoadException) {
                        throw ResourceLoadException("Internal exception: could not load default font", ex)
                    }
                }
            } as TrueTypeFont

            return EngineFontLoader.load("bmp:${path.path.lowercase()}:$pixelHeight") {
                try {
                    return@load generatorFont.generateBitmapFont(pixelHeight)
                } catch (ex: RuntimeException) {
                    throw RuntimeException("Unable to generate bitmap font", ex)
                }
            }
        }
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

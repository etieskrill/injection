package org.etieskrill.engine.graphics.text

import org.joml.Vector2fc

/**
 * A glyph represents a single unicode character belonging to a specific font, where the font family, style and size are
 * defined. This includes the metrics required in order to draw the glyph.
 * <p>
 * A glyph may have a singular corresponding texture by which it should be rendered. Most implementations however use
 * some form of a texture atlas in order to avoid switching textures for each glyph drawn, in which case the
 * {@link Glyph#texture} will be {@code null}.
 *
 * @param size         the visible size of the glyph
 * @param position     offset from the origin point
 * @param advance      the amount of space this glyph actually takes in a line of text
 * @param textureIndex the index in the texture atlas, if any
 * @param character    the character this glyph represents, if any
 */
// * @param texture   the glyph's texture, if any
data class Glyph(
    val size: Vector2fc,
    val position: Vector2fc,
    val advance: Vector2fc,

    val textureIndex: Int, //?,
//    private val texture: Texture2D?,

    val character: Char?
)

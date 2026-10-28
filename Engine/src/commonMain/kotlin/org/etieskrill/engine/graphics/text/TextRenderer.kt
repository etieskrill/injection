package org.etieskrill.engine.graphics.text

import net.bifreus.kml.Mat4c
import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.shader.Shader

expect class TextRenderer {

    constructor(context: GraphicsContext)

    /**
     * Computes the cursor position relative to the [text]'s position given the integer row and column in
     * [cursorPosition] for a given [font] and text wrapping border [size]. Text is not wrapped if [size] is `null`.
     *
     * @param cursorPosition the cursor's desired row and column in [text]
     * @param text           defines the (potentially) irregular text grid
     * @param font           defines the character sizes
     * @param size           if not null, defines the wrapping border
     * @return the relative position, or `null` if [cursorPosition] is not valid in [text]
     */
    fun getAbsoluteCursorPosition(
        cursorPosition: Vec2ic,
        text: String,
        font: Font,
        size: Vec2c? = null
    ): Vec2?

    /**
     * @param cursorPosition will be set to the relative cursor position after rendering the text
     */
    fun render(
        chars: String,
        font: Font,
        position: Vec2c,
        size: Vec2c? = null,
        shader: Shader,
        combined: Mat4c,
        cursorPosition: Vec2? = null,
    )

    /**
     * @return the graphics context this text renderer is bound to
     */
    val context: GraphicsContext

    val renderCalls: Long

}

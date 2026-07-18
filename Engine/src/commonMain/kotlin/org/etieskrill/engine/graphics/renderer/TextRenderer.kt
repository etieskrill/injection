package org.etieskrill.engine.graphics.renderer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.text.Font
import org.joml.Matrix4fc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2ic

expect class TextRenderer {

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
        cursorPosition: Vector2ic,
        text: String,
        font: Font,
        size: Vector2fc? = null
    ): Vector2f?

    /**
     * @param cursorPosition will be set to the relative cursor position after rendering the text
     */
    fun render(
        chars: String,
        font: Font,
        position: Vector2fc,
        size: Vector2fc? = null,
        shader: Shader,
        combined: Matrix4fc,
        cursorPosition: Vector2f? = null,
    )

    /**
     * @return the graphics context this text renderer is bound to
     */
    val context: GraphicsContext

    val renderCalls: Long

}

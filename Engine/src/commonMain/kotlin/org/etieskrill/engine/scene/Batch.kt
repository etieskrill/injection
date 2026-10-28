package org.etieskrill.engine.scene

import net.bifreus.kml.Mat4
import net.bifreus.kml.Mat4c
import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import net.bifreus.kml.div
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.text.TextRenderer
import org.etieskrill.engine.graphics.shader.impl.BlitShader
import org.etieskrill.engine.graphics.shader.impl.TextShader
import org.etieskrill.engine.graphics.shader.impl.UiBoxShader
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.texture.Texture2D

//TODO since everything apart from like, chars in Strings is immediate-mode, this should be renamed
class Batch(
    val frameBuffer: FrameBuffer,
    private val renderer: Renderer,
    private val textRenderer: TextRenderer
) {

    internal val context: GraphicsContext get() = renderer.context

    private val uiBoxPipeline = PostPassPipeline(
        UiBoxShader(),
        frameBuffer,
        opaque = false,
        depthTest = false
    )

    private val blitPipeline = PostPassPipeline(
        BlitShader(),
        frameBuffer,
        opaque = false,
        depthTest = false
    )

    private val uiOutlinePipeline = PostPassPipeline(
        UiOutlineShader(),
        frameBuffer,
        opaque = false,
        depthTest = false
    )

    private val textShader = TextShader()

    var combined: Mat4c = Mat4()
        set(value) {
            (field as Mat4)(value)
        }

    fun render(pipeline: Pipeline<*>) = renderer.render(pipeline)

    /**
     * @param position *CENTER* point of the box
     */
    fun renderCenteredBox(position: Vec2c, size: Vec2c, colour: Vec4c) {
        uiBoxPipeline.shader.let {
            it.position = (size / 2f) + position
            it.size = size
            it.colour = colour
            it.combined = combined
            it.useSprite = false
        }

        renderer.render(uiBoxPipeline)
    }

    fun renderBox(position: Vec2c, size: Vec2c, colour: Vec4c) {
        uiBoxPipeline.shader.let {
            it.position = position
            it.size = size
            it.colour = colour
            it.combined = combined
            it.useSprite = false
        }

        renderer.render(uiBoxPipeline)
    }

    fun getAbsoluteCursorPosition(cursorPosition: Vec2ic, text: String, font: Font, size: Vec2c) =
        textRenderer.getAbsoluteCursorPosition(cursorPosition, text, font, size)

    fun renderBackground(
        position: Vec2c,
        size: Vec2c,
        backgroundColour: Vec4c,
        borderThickness: Float,
        borderColour: Vec4c
    ) {
        uiOutlinePipeline.shader.apply {
            this.position = (position / frameBuffer.size.toFloat()) * Vec2(2f, -2f) - Vec2(1f, -1f)
            this.size = (size / frameBuffer.size.toFloat()) * Vec2(2f, -2f)
            this.backgroundColour = backgroundColour
            this.borderThickness = borderThickness / frameBuffer.size.toFloat()
            this.borderColour = borderColour
        }

        renderer.render(uiOutlinePipeline)
    }

    /**
     * @param size           the borders of the text field, after which the text will be wrapped
     * @param cursorPosition set to the relative cursor position after rendering the text
     */
    fun renderText(
        text: String,
        font: Font,
        position: Vec2c,
        size: Vec2c? = null,
        cursorPosition: Vec2? = null
    ) = textRenderer.render(text, font, position, size, textShader, combined, cursorPosition)

    fun blit(texture: Texture2D, position: Vec2c, size: Vec2c, rotation: Float, colour: Vec4c = Vec4(1f)) {
        blitPipeline.shader.let {
            it.sprite = texture
            it.useSpriteColour = false
            it.position = position
            it.size = size
            it.rotation = rotation
            it.windowSize = frameBuffer.size.toFloat()
            it.colour = colour
        }

        renderer.render(blitPipeline)
    }

    fun ndcToScreenSpace(ndc: Vec2): Vec2 = ((ndc / 2f) + Vec2(0.5)) * frameBuffer.size.toFloat()

}

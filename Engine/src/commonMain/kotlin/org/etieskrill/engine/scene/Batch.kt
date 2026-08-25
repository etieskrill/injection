package org.etieskrill.engine.scene

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.renderer.TextRenderer
import org.etieskrill.engine.graphics.shader.impl.BlitShader
import org.etieskrill.engine.graphics.shader.impl.TextShader
import org.etieskrill.engine.graphics.shader.impl.UiBoxShader
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2ic
import org.joml.Vector4fc
import org.joml.div
import org.joml.plus

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

    var combined: Matrix4fc = Matrix4f()
        set(value) {
            (field as Matrix4f).set(value)
        }

    fun render(pipeline: Pipeline<*>) = renderer.render(pipeline)

    /**
     * @param position *CENTER* point of the box
     */
    fun renderCenteredBox(position: Vector2fc, size: Vector2fc, colour: Vector4fc) {
        uiBoxPipeline.shader.let {
            it.position = position
            it.size = size
            it.colour = colour
            it.combined = combined
            it.useSprite = false
        }

        renderer.render(uiBoxPipeline)
    }

    fun renderBox(position: Vector2fc, size: Vector2fc, colour: Vector4fc) {
        uiBoxPipeline.shader.let {
            it.position = (size / 2f) + position
            it.size = size
            it.colour = colour
            it.combined = combined
            it.useSprite = false
        }

        renderer.render(uiBoxPipeline)
    }

    fun getAbsoluteCursorPosition(cursorPosition: Vector2ic, text: String, font: Font, size: Vector2fc) =
        textRenderer.getAbsoluteCursorPosition(cursorPosition, text, font, size)

    fun renderBackground(
        position: Vector2fc,
        size: Vector2fc,
        backgroundColour: Vector4fc,
        borderThickness: Float,
        borderColour: Vector4fc
    ) {
        uiOutlinePipeline.shader.apply {
            this.position = (position / Vector2f(frameBuffer.size)).mul(2f, -2f).sub(1f, -1f)
            this.size = (size / Vector2f(frameBuffer.size)).mul(2f, -2f)
            this.backgroundColour = backgroundColour
            this.borderThickness = Vector2f(borderThickness) / Vector2f(frameBuffer.size)
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
        position: Vector2fc,
        size: Vector2fc? = null,
        cursorPosition: Vector2f? = null
    ) = textRenderer.render(text, font, position, size, textShader, combined, cursorPosition)

//    val dummyVAO by lazy { GL30C.glGenVertexArrays() } //TODO add to renderer?

    fun blit(texture: Texture2D, position: Vector2fc, size: Vector2fc, rotation: Float, colour: Vector4fc? = null) {
        blitPipeline.shader.let {
            it.sprite = texture
            it.useSpriteColour = true
            it.position = position
            it.size = size
            it.rotation = rotation
            it.windowSize = Vector2f(frameBuffer.size)
            colour?.let { colour -> it.colour = colour }
        }

//        GL30C.glBindVertexArray(dummyVAO) //TODO add to renderer?

        renderer.render(blitPipeline)
    }

    fun ndcToScreenSpace(ndc: Vector2f): Vector2f =
        (ndc / 2f).add(0.5f, 0.5f).mul(frameBuffer.size.x().toFloat(), frameBuffer.size.y().toFloat())

}

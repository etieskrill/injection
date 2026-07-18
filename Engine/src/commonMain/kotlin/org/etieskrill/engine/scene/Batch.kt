package org.etieskrill.engine.scene

import io.github.etieskrill.injection.extension.shader.AbstractShader
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.renderer.TextRenderer
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.text.Font
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2ic
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.Vector4fc
import org.joml.div
import org.joml.plus
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C

//TODO since everything apart from like, chars in Strings is immediate-mode, this should be renamed
class Batch(
    val frameBuffer: FrameBuffer,
    private val renderer: Renderer,
    private val textRenderer: TextRenderer
) {

    var shader: Shader = Shaders.getTextureShader()
    internal val context: GraphicsContext get() = renderer.context
    private val textShader: Shader = Shaders.getTextShader()
    private val blitShader = BlitShader()

    var combined: Matrix4fc = Matrix4f()
        set(value) {
            (field as Matrix4f).set(value)
        }

    private val uiOutlinePipeline = PostPassPipeline(
        UiOutlineShader(),
        frameBuffer,
        opaque = false,
        depthTest = false
    )

    companion object {
        val resetColour = Vector4f(1f)
    }

    fun render(pipeline: Pipeline<*>) = renderer.render(pipeline)

    /**
     * @param position *CENTER* point of the box
     */
    fun renderCenteredBox(position: Vector3fc, size: Vector3fc, colour: Vector4fc) {
        shader.setUniform("colour", colour, false)
        GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA)
        renderer.renderBox(position, size, shader, combined)
        GL11C.glBlendFunc(GL11C.GL_ONE, GL11C.GL_ZERO)
        shader.setUniform("colour", resetColour, false)
    }

    fun renderBox(position: Vector3fc, size: Vector3fc, colour: Vector4fc) {
        shader.setUniform("colour", colour, false)
        val topLeftPosition = (size / 2f) + position
        GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA)
        renderer.renderBox(topLeftPosition, size, shader, combined)
        GL11C.glBlendFunc(GL11C.GL_ONE, GL11C.GL_ZERO)
        shader.setUniform("colour", resetColour, false)
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

    val dummyVAO by lazy { GL30C.glGenVertexArrays() }

    fun blit(texture: Texture2D, position: Vector2fc, size: Vector2fc, rotation: Float, colour: Vector4fc? = null) {
        renderer.context.checkThread()

        blitShader.apply {
            sprite = texture
            useSpriteColour = true
            this.position = position
            this.size = size
            this.rotation = rotation
            windowSize = Vector2f(frameBuffer.size)
            colour?.let { this.colour = colour }
            AbstractShader.start()
        }

        GL30C.glBindVertexArray(dummyVAO)
        GL11C.glDisable(GL11C.GL_DEPTH_TEST)
        GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA)
        GL11C.glDrawArrays(GL11C.GL_TRIANGLE_STRIP, 0, 4)
        GL11C.glBlendFunc(GL11C.GL_ONE, GL11C.GL_ZERO)
    }

    fun ndcToScreenSpace(ndc: Vector2f): Vector2f =
        (ndc / 2f).add(0.5f, 0.5f).mul(frameBuffer.size.x().toFloat(), frameBuffer.size.y().toFloat())

}
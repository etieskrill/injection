package org.etieskrill.engine.graphics.renderer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.buffer.BufferAccessFrequency
import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.text.BitmapFont
import org.etieskrill.engine.graphics.text.Font
import org.joml.Matrix4fc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.lwjgl.opengl.GL11C.*

actual class TextRenderer(
    actual val context: GraphicsContext
) {

    companion object {
        const val MAX_BATCH_LENGTH = 1 shl 10 //Max text length is 1024 characters per draw call
    }

    private var _renderCalls: Long = 0L
    actual val renderCalls: Long get() = _renderCalls

    private val renderedGlyphs: List<RenderedGlyph> = MutableList(MAX_BATCH_LENGTH) { RenderedGlyph() }
    private val glyphVAO: VertexArrayObject<RenderedGlyph> = VertexArrayObject(
        RenderedGlyphAccessor,
        BufferObject(
            RenderedGlyphAccessor,
            MAX_BATCH_LENGTH,
            accessFrequency = BufferAccessFrequency.STREAM
        ),
        null
    )

    actual fun getAbsoluteCursorPosition(
        cursorPosition: Vector2ic,
        text: String,
        font: Font,
        size: Vector2fc?
    ): Vector2f? {
        if (cursorPosition.equals(0, 0)) return Vector2f(0f)

        var numChars = 0
        val pen = Vector2f(0f)
        val currentCursorPosition = Vector2i(0)

        for (glyph in font.getGlyphs(text)) {
            when (glyph.character) {
                '\n' -> {
                    pen.set(0f, pen.y + font.lineHeight)
                    currentCursorPosition.x = 0
                    currentCursorPosition.y++
                }
                else -> {
                    if (size != null && (pen.x + glyph.advance.x() > size.x())) { //special chars probably shouldn't get wrapped, right?
                        pen.set(0f, pen.y + font.lineHeight)
                        currentCursorPosition.x = 0
                        currentCursorPosition.y++
                    }

                    currentCursorPosition.x++
                    pen.add(glyph.advance)
                }
            }

            numChars++

            if (currentCursorPosition == cursorPosition || numChars == text.length) return pen
            else if (numChars > text.length) return null
        }

        return null
    }

    actual fun render(
        chars: String,
        font: Font,
        position: Vector2fc,
        size: Vector2fc?,
        shader: Shader,
        combined: Matrix4fc,
        cursorPosition: Vector2f?
    ) {
        context.checkThread()

        GLUtils.clearError()

        shader.setUniform("combined", combined)
        shader.setUniform("glyphTextureSize", Vector2f(font.pixelSize))

        when (font) {
            is BitmapFont -> renderBitmap(chars, font, position, size, shader, cursorPosition)
            else -> error("Rendering font of type ${font::class.simpleName} is not supported") //TODO ttf
        }

        GLUtils.checkError("Error drawing glyphs")
    }

    private fun renderBitmap(
        chars: String,
        font: BitmapFont,
        position: Vector2fc,
        size: Vector2fc?,
        shader: Shader,
        cursorPosition: Vector2f?
    ) {
        var numRenderableChars = 0
        var renderedGlyphIndex = 0
        val pen = Vector2f(0f)
        val penPosition = Vector2f(0f)

        for (glyph in font.getGlyphs(chars)) {
            when (glyph.character) {
                '\n' -> {
                    pen.set(0f, pen.y() + font.lineHeight)
                    continue
                }
                else -> {
                    //TODO this is only the most primitive of wrapping; maybe add wrap mode enum? callback, even?
                    if (size != null && (pen.x + glyph.advance.x() > size.x())) { //special chars probably shouldn't get wrapped, right?
                        pen.set(0f, pen.y + font.lineHeight)
                    }
                    numRenderableChars++
                }
            }

            penPosition
                    .set(position).add(pen)
                    .add(glyph.position)

            val renderedGlyph = renderedGlyphs[renderedGlyphIndex++]
            renderedGlyph.size.set(glyph.size)
            renderedGlyph.position.set(penPosition)
            renderedGlyph.textureIndex = glyph.textureIndex

            pen.add(glyph.advance)
        }

        cursorPosition?.set(pen)

        renderBitmapGlyphs(numRenderableChars, renderedGlyphs, font, shader)
    }

    private fun renderBitmapGlyphs(
        numChars: Int,
        renderedGlyphs: List<RenderedGlyph>,
        font: BitmapFont,
        shader: Shader
    ) {
        glyphVAO.vertices = renderedGlyphs //TODO replace with ssbo?
        shader.setTexture("glyphs", font.textures)

        context.getVertexArray(glyphVAO).bind()
        context.getShader(shader).bind()

//        val glyphPipeline = new Pipeline(
//                glyphVAO,
//                PipelineConfig(
//                        alphaMode = SOURCE_ALPHA,
//                        primitiveType = POINTS,
//                        cullingMode = NONE,
//                        depthTest = false,
//                        writeDepth = false
//                ),
//                shader, context.screenBuffer???)
//
//        renderer.render(glyphPipeline)

        context.withContext {
            glDisable(GL_CULL_FACE)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glDrawArrays(GL_POINTS, 0, numChars)
            glBlendFunc(GL_ONE, GL_ZERO)
            glEnable(GL_CULL_FACE)
        }

        _renderCalls++
    }

}

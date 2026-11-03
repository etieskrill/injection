package org.etieskrill.engine.graphics.text

import net.bifreus.kml.Mat4c
import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.buffer.BufferAccessFrequency
import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.pipeline.AlphaMode
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.renderer.RenderedGlyph
import org.etieskrill.engine.graphics.renderer.RenderedGlyphAccessor
import org.etieskrill.engine.graphics.shader.Shader
import org.lwjgl.opengl.GL11C

actual class TextRenderer actual constructor(
    actual val context: GraphicsContext
) {

    companion object {
        const val MAX_BATCH_LENGTH = 1 shl 10 //Max text length is 1024 characters per draw call
    }

    private var _renderCalls: Long = 0L
    actual val renderCalls: Long get() = _renderCalls

    //TODO more statistics

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
        cursorPosition: Vec2ic,
        text: String,
        font: Font,
        size: Vec2c?
    ): Vec2? {
        if (cursorPosition == Vec2i.ZERO) return Vec2(0f)

        var numChars = 0
        val pen = Vec2(0f)
        val currentCursorPosition = Vec2i(0)

        for (glyph in font.getGlyphs(text)) {
            when (glyph.character) {
                '\n' -> {
                    pen(0f, pen.y + font.lineHeight)
                    currentCursorPosition.x = 0
                    currentCursorPosition.y++
                }
                else -> {
                    if (size != null && (pen.x + glyph.advance.x > size.x)) { //special chars probably shouldn't get wrapped, right?
                        pen(0f, pen.y + font.lineHeight)
                        currentCursorPosition.x = 0
                        currentCursorPosition.y++
                    }

                    currentCursorPosition.x++
                    pen(pen + glyph.advance)
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
        position: Vec2c,
        size: Vec2c?,
        shader: Shader,
        combined: Mat4c,
        cursorPosition: Vec2?
    ) {
        context.checkThread()

        GLUtils.clearError()

        shader.setUniform("combined", combined)
        shader.setUniform("glyphTextureSize", font.pixelSize.toFloat())

        when (font) {
            is BitmapFont -> renderBitmap(chars, font, position, size, shader, cursorPosition)
            else -> error("Rendering font of type ${font::class.simpleName} is not supported") //TODO ttf
        }

        GLUtils.checkError("Error drawing glyphs")
    }

    private fun renderBitmap(
        chars: String,
        font: BitmapFont,
        position: Vec2c,
        size: Vec2c?,
        shader: Shader,
        cursorPosition: Vec2?
    ) {
        var numRenderableChars = 0
        var renderedGlyphIndex = 0
        val pen = Vec2(0f)
        val penPosition = Vec2(0f)

        for (glyph in font.getGlyphs(chars)) {
            when (glyph.character) {
                '\n' -> {
                    pen(0f, pen.y + font.lineHeight)
                    continue
                }
                else -> {
                    //TODO this is only the most primitive of wrapping; maybe add wrap mode enum? callback, even?
                    if (size != null && (pen.x + glyph.advance.x > size.x)) { //special chars probably shouldn't get wrapped, right?
                        pen(0f, pen.y + font.lineHeight)
                    }
                    numRenderableChars++
                }
            }

            penPosition(position + pen + glyph.position)

            val renderedGlyph = renderedGlyphs[renderedGlyphIndex++]
            renderedGlyph.size(glyph.size)
            renderedGlyph.position(penPosition)
            renderedGlyph.textureIndex = glyph.textureIndex

            pen(pen + glyph.advance)
        }

        cursorPosition?.invoke(pen)

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
            context.cullingMode = CullingMode.NONE
            context.alphaMode = AlphaMode.SOURCE_ALPHA
            GL11C.glDrawArrays(GL11C.GL_POINTS, 0, numChars)
        }

        _renderCalls++
    }

}

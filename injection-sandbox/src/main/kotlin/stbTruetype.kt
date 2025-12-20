import SDFTextShader.FontPoint
import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.etieskrill.engine.graphics.gl.VertexArrayObject
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.text.BitmapFont
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.text.Glyph
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.etieskrill.engine.util.ResourceReader.getRawResource
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.opengl.GL11C.GL_CULL_FACE
import org.lwjgl.opengl.GL11C.GL_LINE_STRIP
import org.lwjgl.opengl.GL11C.GL_ONE
import org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11C.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11C.GL_ZERO
import org.lwjgl.opengl.GL11C.glBlendFunc
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack.stackPush
import java.nio.ByteBuffer

fun main() = STBTTTest.run()

object STBTTTest : App() {

    val sdfTextShader = SDFTextShader()
    val batch = Batch(renderer, renderer, window.currentSize, null, sdfTextShader.shader as ShaderProgram, null)

    val font: Font

    val texture: org.etieskrill.engine.graphics.texture.Texture2D

    init {
        val info = STBTTFontinfo.create()
        val buffer = getRawResource("fonts/AGENCYR.TTF")

        if (!stbtt_InitFont(info, buffer)) throw RuntimeException("Failed to init font")

        val lineHeight = 64f

        val scale = stbtt_ScaleForPixelHeight(info, lineHeight)

        val ascent: Float
        val descent: Float
        val lineGap: Int
        stackPush().use {
            val stackAscent = it.mallocInt(1)
            val stackDescent = it.mallocInt(1)
            val stackLineGap = it.mallocInt(1)
            stbtt_GetFontVMetrics(info, stackAscent, stackDescent, stackLineGap)
            ascent = stackAscent[0] * scale
            descent = stackDescent[0] * scale
            lineGap = stackLineGap[0]
        }

        val width: Int
        val height: Int
        val x: Int
        val y: Int
        val bitmap: ByteBuffer
        stackPush().use {
            val stackWidth = it.mallocInt(1)
            val stackHeight = it.mallocInt(1)
            val stackX = it.mallocInt(1)
            val stackY = it.mallocInt(1)
            bitmap = stbtt_GetCodepointSDF(
                /* font = */ info,
                /* scale = */ scale,
                /* codepoint = */ 'A'.code,
                /* padding = */ 10,
                /* onedge_value = */ 127,
                /* pixel_dist_scale = */ 10f,
                /* width = */ stackWidth,
                /* height = */ stackHeight,
                /* xoff = */ stackX,
                /* yoff = */ stackY
            )
                ?: throw RuntimeException("Failed to get code point")
            width = stackWidth[0]
            height = stackHeight[0]
            x = stackX[0]
            y = stackY[0]
        }

//        if (!stbi_write_png("test.png", width, height, 1, bitmap, 0))
//            throw IllegalStateException("Image saving did not complete successfully: ${stbi_failure_reason()}")

        font = object : BitmapFont(
            mapOf('A' to Glyph(Vector2f(width.toFloat(), height.toFloat()), Vector2f(x.toFloat(), y.toFloat()), Vector2f(0f), 0, 'A')),
            lineHeight.toInt(), lineHeight.toInt(), Vector2i(width, height), "fuck", "you", null
        ) {
            private val glyph = Glyph(
                Vector2f(width.toFloat(), height.toFloat()),
                Vector2f(x.toFloat(), y.toFloat()),
                Vector2f(0f),
                0,
                'A'
            )

            override fun getGlyph(c: Char) = glyph
        }

        texture = org.etieskrill.engine.graphics.texture.Texture2D
            .BufferBuilder(bitmap, Vector2i(width, height), AbstractTexture.Format.ALPHA)
            .build()
    }

    val vao = VertexArrayObject.builder<FontPoint>(FontPointAccessor)
        .setVertexElements(arrayListOf(FontPoint(
            Vector2f(0f, 0f),
            Vector2f(texture.size),
            Vector2f(0f),
            Vector2f(1f)
        )))
        .build()

    override fun loop(delta: Double) {
//        batch.renderText("A", font, Vector2f(100f))

        sdfTextShader.fontTexture = texture
        sdfTextShader.combined = batch.combined

        glDisable(GL_CULL_FACE)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDrawArrays(GL_LINE_STRIP, 0, 4)
        glBlendFunc(GL_ONE, GL_ZERO)
        glEnable(GL_CULL_FACE)
    }

}

object FontPointAccessor : VertexArrayAccessor<FontPoint>() {
    override fun registerFields() {
        addField(Vector2f::class.java) { point, buffer -> point.position.get(buffer) }
        addField(Vector2f::class.java) { point, buffer -> point.size.get(buffer) }
        addField(Vector2f::class.java) { point, buffer -> point.atlasPos.get(buffer) }
        addField(Vector2f::class.java) { point, buffer -> point.atlasSize.get(buffer) }
    }
}

class SDFTextShader : ShaderBuilder<FontPoint, SDFTextShader.Vertex, ColourRenderTarget>(
    object : ShaderProgram(listOf("SDFText.glsl")) {}
) {
    data class FontPoint(val position: vec2, val size: vec2, val atlasPos: vec2, val atlasSize: vec2)
    data class Vertex(override val position: vec4, val texCoord: vec2) : ShaderVertexData

    val vertices by const(arrayOf(vec2(0, 0), vec2(1, 0), vec2(0, 1), vec2(1, 1)))

    var combined by uniform<mat4>()
    var fontTexture by uniform<sampler2D>()

    override fun program() {
        vertex {
            val cornerIndex = (vertexID % 4).toInt()
            val corner = vertices[cornerIndex]
            val position = combined * vec4(it.position + it.size * corner, 0, 1)
            Vertex(position, vec2(it.atlasPos + it.atlasSize * corner))
        }
        fragment {
            val opacity = texture(fontTexture, it.texCoord).r
            ColourRenderTarget(vec4(1, 1, 1, opacity).rt)
        }
    }
}

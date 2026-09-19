package org.etieskrill.engine.graphics.text

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.common.ResourceLoadException
import org.etieskrill.engine.graphics.texture.ArrayTexture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureRowAlignment
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.etieskrill.engine.util.readResource
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.BufferUtils.createPointerBuffer
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FreeType.*

private val logger = KotlinLogging.logger {}

//TODO expectify patronum
private var library: Long? = null

class TrueTypeFont(
    file: String
) : Font, Disposable {

    //TODO expectify patronum
    private val face: FT_Face
    val scalable: Boolean
    val sizes: IntArray

    //TODO expectify patronum
    override val lineHeight: Int get() = face.size()!!.metrics().height().toInt() / 64
    override val minLineHeight: Int get() = face.size()!!.metrics().run { (ascender() - descender()) / 64 }.toInt()

    val family: String
    val style: String

    override val pixelSize: Vector2i

    //TODO expectify patronum
    init {
        initLibrary()

        val faceBuffer = createPointerBuffer(1)
        val fontFile = readResource(file).run { createByteBuffer(size).put(this).flip() }
        check(
            FT_New_Memory_Face(library!!, fontFile, 0, faceBuffer),
            "Font could not be loaded from file '$file'"
        )

        face = FT_Face.create(faceBuffer.get())

        scalable = (face.face_flags() and FT_FACE_FLAG_SCALABLE.toLong()) != 0L

        if (face.num_fixed_sizes() > 0) {
            face.available_sizes()!!.let { buffer ->
                sizes = IntArray(face.num_fixed_sizes()) { buffer.get().height().toInt() }
            }
        } else {
            sizes = intArrayOf()
        }

        family = face.family_nameString()
        style = face.style_nameString()

        pixelSize = Vector2i(Font.INVALID_PIXEL_SIZE)
    }

    private fun initLibrary() {
        if (library != null) return
        val libBuffer = createPointerBuffer(1)
        check(FT_Init_FreeType(libBuffer), "Unable to initialise FreeType library")
        library = libBuffer.get()
    }

    //TODO expectify patronum
    //TODO bitmap texture packer
    fun generateBitmapFont(pixelHeight: Int, pixelWidth: Int = pixelHeight, verticalUp: Boolean = false): BitmapFont {
        require(pixelHeight > 0) { "Font height must be greater than zero" }
        require(pixelWidth > 0) { "Font width must be greater than zero" }
        check(!disposed) { "Cannot generate bitmap as freetype resource was already disposed" }

        //Is FT_Set_Char_Size ever going to be more practical than this?
        check(
            FT_Set_Pixel_Sizes(face, pixelWidth, pixelHeight),
            "Failed to set font pixel size"
        )

        var glyphIndex = 0

        //Currently, if any glyph is wider than it is tall, this will cause everything to break TODO maybe not?
        pixelSize.set(pixelWidth, pixelHeight)

        if (128 < face.num_glyphs().toInt()) {
            logger.warn {
                "Font contains ${face.num_glyphs()} glyphs. Only ASCII fonts are supported right now, and any glyphs" +
                        " above index ${Font.NUM_CHARS_ASCII} are ignored"
            }
        }
        val textureBuffer = ByteArray(pixelWidth * pixelHeight * Font.NUM_CHARS_ASCII)
        var bufferHead = 0

        val glyphs = mutableMapOf<Char, Glyph>()
        for (i in 0L until Font.NUM_CHARS_ASCII) {
            check(
                FT_Load_Char(face, i, FT_LOAD_RENDER),
                "Could not load character \"${i.toInt().toChar()}\""
            )

            val ftGlyph = face.glyph()
            if (ftGlyph == null) {
                logger.warn { "Failed to load character \"${i.toInt().toChar()}\" from face" }
                continue
            }
            //despite the enticing suggestion by intellisense, do NOT close these resources, it causes significant pain
            val bitmap = ftGlyph.bitmap()
            val adv = ftGlyph.advance()

            val size = Vector2f(bitmap.width().toFloat(), bitmap.rows().toFloat())
            val position = Vector2f(ftGlyph.bitmap_left().toFloat(), ftGlyph.bitmap_top().toFloat())
            if (!verticalUp) position.mul(1f, -1f).add(0f, minLineHeight.toFloat())
            val advance = Vector2f(adv.x().toFloat(), adv.y().toFloat()).mul(1f / 64f)
            if (!verticalUp) advance.mul(1f, -1f)

            val buffer = bitmap.buffer((size.x() * size.y()).toInt())
            if (buffer != null) { //Pad top and right of buffer to specified pixel size
                for (j in 0 until size.y().toInt()) {
                    for (k in 0 until size.x().toInt()) textureBuffer[bufferHead++] = buffer.get()
                    bufferHead += pixelWidth - size.x().toInt()
                }
                bufferHead += (pixelHeight - size.y().toInt()) * pixelWidth
            } else {
                logger.trace {
                    "Encountered glyph '${i.toInt().toChar()}' (code: $i) without buffer, proceeding with blank bitmap"
                }
                bufferHead += pixelWidth * pixelHeight
            }

            glyphs[i.toInt().toChar()] = Glyph(size, position, advance, glyphIndex++, i.toInt().toChar())
        }

        val texture = ArrayTexture2D(
            Vector2i(pixelWidth, pixelHeight),
            Font.NUM_CHARS_ASCII,
            TextureType.DIFFUSE, //TODO raw?
            format = TextureFormat.ALPHA,
            minFilter = TextureMinFilter.NEAREST, //TODO min nearest is better for small text, but when rendering using pixel sizes this should never be used anyway
            wrapping = TextureWrapping.CLAMP_TO_BORDER,
            rowAlignment = TextureRowAlignment.BYTE,
            buffer = textureBuffer
        )

        //TODO figure out whether the face height is actually scaled via FT_Set_Pixel_Sizes or the like
        // alr it seems like it does not, but how to get the height then?

        return BitmapFont(
            glyphs,
            lineHeight,
            minLineHeight,
            pixelSize,
            face.family_nameString(),
            face.style_nameString(),
            texture
        )
    }

    override fun getGlyph(c: Char): Glyph {
        if (FT_Load_Char(face, c.code.toLong(), FT_LOAD_RENDER) != FT_Err_Ok) {
            TODO("Not yet implemented")
        }
        TODO("Not yet implemented")
    }

    override fun getGlyphs(s: String): Array<Glyph> {
        TODO("Not yet implemented")
    }

    private var disposed = false

    override fun dispose() {
        if (disposed) return
        check(FT_Done_Face(face), "Failed to release font resource")
        disposed = true
    }

}

fun disposeLibrary() = library?.let {
    check(FT_Done_Library(it), "Failed to release freetype library")
    library = null
}

@OptIn(ExperimentalStdlibApi::class)
private fun check(retCode: Int, failureMessage: String) {
    if (retCode != FT_Err_Ok) {
        throw ResourceLoadException("$failureMessage: 0x${retCode.toHexString()} ${FT_Error_String(retCode)}")
    }
}

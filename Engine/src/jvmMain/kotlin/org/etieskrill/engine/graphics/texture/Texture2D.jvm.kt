package org.etieskrill.engine.graphics.texture

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType
import org.etieskrill.engine.graphics.framebuffer.gl
import org.joml.Vector2ic
import org.joml.Vector4fc
import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C.*
import io.github.etieskrill.injection.extension.shader.Texture2D as DslTexture2D

private val logger = KotlinLogging.logger {}

actual class Texture2D actual constructor(
    context: GraphicsContext,
    actual override val size: Vector2ic,
    private var textureData: ByteArray?,
    format: TextureFormat,
    type: TextureType,
    minFilter: TextureMinFilter,
    magFilter: TextureMagFilter,
    wrapping: TextureWrapping,
    borderColour: Vector4fc,
) : Texture(context, format, type, minFilter, magFilter, wrapping, borderColour), DslTexture2D, FrameBufferAttachment {

    override val glTarget: Int get() = GL_TEXTURE_2D

    actual companion object {
        actual fun createBlank(context: GraphicsContext, size: Vector2ic, format: TextureFormat) =
            Texture2D(context, size, format = format)

        actual fun createFromBuffer(context: GraphicsContext, size: Vector2ic, buffer: ByteArray, format: TextureFormat) =
            Texture2D(context, size, buffer, format = format)

        actual fun createFromFile(file: String, context: GraphicsContext, type: TextureType): Texture2D {
            val textureData = loadTexture2DData(file, type)
            return Texture2D(context, textureData.size, textureData.buffer, textureData.format, type)
        }
    }

    override fun bufferTextureData() = context.withContext {
        val bytesExpected = size.x() * size.y() * format.numChannels
        check(textureData == null || textureData!!.size == bytesExpected) {
            "Texture data buffer contains ${textureData!!.size} bytes when $bytesExpected bytes were expected"
        }

        bind(0)
        //TODO what is this for?
        val texelFormat = if (format != TextureFormat.DEPTH_STENCIL) GL_UNSIGNED_BYTE else GL_UNSIGNED_INT_24_8

        val data = textureData?.let { createByteBuffer(it.size).put(it).flip() }
        textureData = null
        glTexImage2D(
            glTarget, 0, format.glInternal,
            size.x(), size.y(),
            0, format.gl, texelFormat, data
        )

        logger.debug {
            "Loaded ${size.x()}x${size.y()} ${8 * format.numChannels}-bit ${format.name.lowercase()} ${type.name.lowercase()} texture"
        }
    }

    override fun attach(frameBuffer: FrameBuffer, type: FrameBufferAttachmentType) = context.withContext {
        frameBuffer.bind()
        glFramebufferTexture2D(GL_FRAMEBUFFER, type.gl, glTarget, id, 0)
    }

}

package org.etieskrill.engine.graphics.texture

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType
import org.etieskrill.engine.graphics.framebuffer.FrameBufferInstance
import org.etieskrill.engine.graphics.framebuffer.gl
import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C.*

private val logger = KotlinLogging.logger {}

internal actual class Texture2DInstance(
    descriptor: Texture2D,
    context: GraphicsContext
) : TextureInstance<Texture2D>(descriptor, context), FrameBufferAttachmentInstance {

    override val glTarget: Int get() = GL_TEXTURE_2D

    override fun bufferTextureData() = context.withContext {
        val bytesExpected = descriptor.size.x() * descriptor.size.y() * descriptor.format.numChannels

        check(descriptor.buffer == null || descriptor.buffer.size == bytesExpected) {
            "Texture data buffer contains ${descriptor.buffer!!.size} bytes when $bytesExpected bytes were expected"
        }

        bind(0)
        val texelFormat = when (descriptor.format) {
            TextureFormat.DEPTH_STENCIL -> GL_UNSIGNED_INT_24_8
            else -> GL_UNSIGNED_BYTE
        }

        val data = when {
            descriptor.buffer != null -> createByteBuffer(descriptor.buffer.size).put(descriptor.buffer).flip()
            descriptor.file != null -> {
                val textureData = loadTexture2DData(descriptor.file, descriptor.type)
                val bufferSize = textureData.size.x() * textureData.size.y() * textureData.format.numChannels
                check(bufferSize == bytesExpected)
                createByteBuffer(bufferSize).put(textureData.buffer).flip()
            }

            else -> null
        }

        glTexImage2D(
            glTarget, 0, descriptor.format.glInternal,
            descriptor.size.x(), descriptor.size.y(),
            0, descriptor.format.gl, texelFormat, data
        )

        logger.debug {
            "Loaded ${descriptor.size.x()}x${descriptor.size.y()} ${8 * descriptor.format.numChannels}-bit ${
                descriptor.format.name.lowercase()
            } ${descriptor.type.name.lowercase()} texture"
        }
    }

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) = context.withContext {
        glFramebufferTexture2D(GL_FRAMEBUFFER, type.gl, glTarget, id, 0)
    }

}

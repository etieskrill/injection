package org.etieskrill.engine.graphics.texture;

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL12C.glTexImage3D
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY

private val logger = KotlinLogging.logger {}

internal actual class ArrayTexture2DInstance(
    descriptor: ArrayTexture2D,
    context: GraphicsContext
) : TextureInstance<ArrayTexture2D>(descriptor, context) {

    override val glTarget: Int get() = GL_TEXTURE_2D_ARRAY

    override fun bufferTextureData() = context.withContext {
        val bytesExpected = descriptor.run { size.x() * size.y() * format.numChannels * length }

        check(descriptor.buffer == null || descriptor.buffer.size == bytesExpected) {
            "Array texture data buffer contains ${descriptor.buffer!!.size} bytes when $bytesExpected bytes were expected"
        }

        bind(0)

        val data = when {
            descriptor.buffer != null -> createByteBuffer(descriptor.buffer.size).put(descriptor.buffer).flip()
            descriptor.file != null -> TODO("probably makes no sense?")
            else -> null
        }

        glTexImage3D(
            glTarget, 0, descriptor.format.glInternal,
            descriptor.size.x(), descriptor.size.y(), descriptor.length,
            0, descriptor.format.gl, GL_UNSIGNED_BYTE, data
        )

        logger.debug {
            "Loaded ${descriptor.size.x()}x${descriptor.size.y()} ${8 * descriptor.format.numChannels}-bit ${
                descriptor.type.name.lowercase()
            } array texture containing ${descriptor.length} elements"
        }
    }

}

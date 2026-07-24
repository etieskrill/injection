package org.etieskrill.engine.graphics.texture;

import io.github.etieskrill.injection.extension.shader.Texture2DArray
import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.joml.Vector2ic
import org.joml.Vector4fc
import org.lwjgl.BufferUtils
import org.lwjgl.BufferUtils.createByteBuffer
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL12C.glTexImage3D
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY

private val logger = KotlinLogging.logger {}

actual class ArrayTexture2D actual constructor(
    context: GraphicsContext,
    actual val size: Vector2ic,
    actual val length: Int,
    private var textureData: ByteArray?,
    type: TextureType,
    format: TextureFormat,
    minFilter: TextureMinFilter,
    magFilter: TextureMagFilter,
    wrapping: TextureWrapping,
    borderColour: Vector4fc
) : Texture(context, format, type, minFilter, magFilter, wrapping, borderColour), Texture2DArray {

    override val glTarget: Int get() = GL_TEXTURE_2D_ARRAY

    override fun bufferTextureData() = context.withContext {
        val bytesExpected = size.x() * size.y() * format.numChannels * length
        check(textureData == null || textureData!!.size == bytesExpected) {
            "Array texture data buffer contains ${textureData!!.size} bytes when $bytesExpected bytes were expected"
        }

        bind(0)

        val data = textureData?.let { createByteBuffer(it.size).put(it).flip() }
        textureData = null
        glTexImage3D(
            glTarget, 0, format.glInternal,
            size.x(), size.y(), length,
            0, format.gl, GL_UNSIGNED_BYTE, data
        )

        logger.debug {
            "Loaded ${size.x()}x${size.y()} ${8 * format.numChannels}-bit ${type.name.lowercase()} array texture containing $length elements"
        }
    }

}

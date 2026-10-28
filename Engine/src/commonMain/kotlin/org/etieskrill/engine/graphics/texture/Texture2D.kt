package org.etieskrill.engine.graphics.texture

import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import org.etieskrill.engine.graphics.texture.TextureMagFilter.LINEAR
import org.etieskrill.engine.graphics.texture.TextureMinFilter.TRILINEAR
import io.github.etieskrill.injection.extension.shader.Texture2D as DslTexture2D

class Texture2D(
    override val size: Vec2ic,
    type: TextureType,
    internal val buffer: ByteArray? = null,
    internal val file: String? = null,
    format: TextureFormat,
    minFilter: TextureMinFilter = TRILINEAR,
    magFilter: TextureMagFilter = LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    borderColour: Vec4c = Vec4(0f),
    rowAlignment: TextureRowAlignment = TextureRowAlignment.WORD
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour, rowAlignment), DslTexture2D,
    FrameBufferAttachment {

    init {
        check(buffer == null || file == null) { "File and buffer cannot both be set" } //TODO add debug/release build check facilities
    }

    companion object {
        fun createBlank(size: Vec2ic, format: TextureFormat): Texture2D =
            Texture2D(size, TextureType.UNKNOWN, format = format)

        fun createFromBuffer(size: Vec2ic, buffer: ByteArray, format: TextureFormat): Texture2D =
            Texture2D(size, TextureType.UNKNOWN, buffer = buffer, format = format)

        fun createFromFile(file: String, type: TextureType): Texture2D {
            val textureData = loadTexture2DData(file, type) //TODO replace with size-only reader
            return Texture2D(textureData.size, type, file = file, format = textureData.format)
        }

        fun createUITexture(file: String): Texture2D {
            val textureData = loadTexture2DData(file, TextureType.DIFFUSE) //TODO replace with size-only reader
            return Texture2D(
                textureData.size,
                TextureType.DIFFUSE,
                textureData.buffer,
                format = textureData.format,
                minFilter = TextureMinFilter.LINEAR
            )
        }
    }

}

internal expect class Texture2DInstance : TextureInstance<Texture2D>, FrameBufferAttachmentInstance

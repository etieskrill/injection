package org.etieskrill.engine.graphics.texture

import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import io.github.etieskrill.injection.extension.shader.TextureCubeMapArray as DslTextureCubeMapArray

class TextureCubeMapArray(
    override val size: Vec2ic,
    val length: Int,
    type: TextureType,
    internal val buffer: ByteArray? = null,
    internal val file: String? = null,
    format: TextureFormat,
    minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
    magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    borderColour: Vec4c = Vec4(0f),
    rowAlignment: TextureRowAlignment = TextureRowAlignment.WORD
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour, rowAlignment),
    DslTextureCubeMapArray, FrameBufferAttachment {
    init {
        check(buffer == null || file == null) { "File and buffer cannot both be set" } //TODO add debug/release build check facilities
    }
}

internal expect class TextureCubeMapArrayInstance : TextureInstance<TextureCubeMapArray>, FrameBufferAttachmentInstance

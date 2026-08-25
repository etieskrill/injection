package org.etieskrill.engine.graphics.texture

import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc
import io.github.etieskrill.injection.extension.shader.TextureCubeMapArray as DslTextureCubeMapArray

class TextureCubeMapArray(
    val size: Vector2ic,
    val length: Int,
    type: TextureType,
    internal val buffer: ByteArray?,
    internal val file: String?,
    format: TextureFormat,
    minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
    magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    borderColour: Vector4fc = Vector4f(0f)
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour),
    DslTextureCubeMapArray /*, FrameBufferAttachment*/ {
    init {
        check(buffer == null || file == null) { "File and buffer cannot both be set" } //TODO add debug/release build check facilities
    }
}

internal expect class TextureCubeMapArrayInstance : TextureInstance<TextureCubeMapArray>

package org.etieskrill.engine.graphics.texture

import io.github.etieskrill.injection.extension.shader.Texture2DArray
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc

class ArrayTexture2D(
    val size: Vector2ic,
    val length: Int,
    type: TextureType,
    internal val buffer: ByteArray? = null,
    internal val file: String? = null,
    format: TextureFormat,
    minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
    magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    borderColour: Vector4fc = Vector4f(0f)
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour), Texture2DArray {
    init {
        check(buffer == null || file == null) { "File and buffer cannot both be set" } //TODO add debug/release build check facilities
    }
}

internal expect class ArrayTexture2DInstance : TextureInstance<ArrayTexture2D>

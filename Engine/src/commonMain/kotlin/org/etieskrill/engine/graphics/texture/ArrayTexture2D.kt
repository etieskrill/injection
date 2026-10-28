package org.etieskrill.engine.graphics.texture

import io.github.etieskrill.injection.extension.shader.Texture2DArray
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c

class ArrayTexture2D(
    val size: Vec2ic,
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
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour, rowAlignment), Texture2DArray {
    init {
        check(buffer == null || file == null) { "File and buffer cannot both be set" } //TODO add debug/release build check facilities
    }
}

internal expect class ArrayTexture2DInstance : TextureInstance<ArrayTexture2D>

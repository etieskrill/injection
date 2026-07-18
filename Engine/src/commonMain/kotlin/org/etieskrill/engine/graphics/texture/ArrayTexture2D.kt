package org.etieskrill.engine.graphics.texture

import io.github.etieskrill.injection.extension.shader.Texture2DArray
import org.etieskrill.engine.graphics.GraphicsContext
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc

expect class ArrayTexture2D : Texture, Texture2DArray {

    val size: Vector2ic
    val length: Int

    constructor(
        context: GraphicsContext,
        size: Vector2ic,
        length: Int,
        textureData: ByteArray? = null,
        type: TextureType = TextureType.UNKNOWN,
        format: TextureFormat? = null,
        minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
        magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
        wrapping: TextureWrapping = TextureWrapping.REPEAT,
        borderColour: Vector4fc = Vector4f(0f)
    )

}

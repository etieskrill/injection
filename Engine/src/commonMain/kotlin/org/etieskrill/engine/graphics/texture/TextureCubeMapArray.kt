package org.etieskrill.engine.graphics.texture

import org.etieskrill.engine.graphics.GraphicsContext
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc
import io.github.etieskrill.injection.extension.shader.TextureCubeMapArray as DslTextureCubeMapArray

expect class TextureCubeMapArray : Texture, DslTextureCubeMapArray /*, FrameBufferAttachment*/ {

    val size: Vector2ic
    val length: Int

    constructor(
        context: GraphicsContext,
        size: Vector2ic,
        length: Int,
        format: TextureFormat,
        type: TextureType = TextureType.UNKNOWN,
        minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
        magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
        wrapping: TextureWrapping = TextureWrapping.REPEAT,
        borderColour: Vector4fc = Vector4f(0f)
    )

}

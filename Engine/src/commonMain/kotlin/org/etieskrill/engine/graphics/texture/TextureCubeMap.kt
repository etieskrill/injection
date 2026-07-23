package org.etieskrill.engine.graphics.texture

import org.etieskrill.engine.graphics.GraphicsContext
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc
import io.github.etieskrill.injection.extension.shader.TextureCubeMap as DslTextureCubeMap

expect class TextureCubeMap : Texture, DslTextureCubeMap /*, FrameBufferAttachment */ {

    val size: Vector2ic

    companion object {
        val NUM_SIDES: Int

        fun createBlank(context: GraphicsContext, size: Vector2ic, format: TextureFormat): TextureCubeMap
        fun createFromFile(file: String, context: GraphicsContext, type: TextureType): TextureCubeMap
        fun createFromBuffer(
            context: GraphicsContext, size: Vector2ic, buffer: List<ByteArray>, format: TextureFormat
        ): TextureCubeMap
    }

    constructor(
        context: GraphicsContext,
        size: Vector2ic,
        textureData: List<ByteArray>? = null,
        format: TextureFormat,
        type: TextureType = TextureType.UNKNOWN,
        minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
        magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
        wrapping: TextureWrapping = TextureWrapping.REPEAT,
        borderColour: Vector4fc = Vector4f(0f)
    )

}

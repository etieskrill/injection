package org.etieskrill.engine.graphics.texture

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.ByteArray
import io.github.etieskrill.injection.extension.shader.Texture2D as DslTexture2D

expect class Texture2D : Texture, DslTexture2D, FrameBufferAttachment {

    override val size: Vector2ic

    constructor(
        context: GraphicsContext,
        size: Vector2ic,
        textureData: ByteArray? = null,
        format: TextureFormat,
        type: TextureType = TextureType.UNKNOWN,
        minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
        magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
        wrapping: TextureWrapping = TextureWrapping.REPEAT,
        borderColour: Vector4fc = Vector4f(0f)
    )

    companion object {
        fun createBlank(context: GraphicsContext, size: Vector2ic, format: TextureFormat): Texture2D
        fun createFromBuffer(context: GraphicsContext, size: Vector2ic, buffer: ByteArray, format: TextureFormat): Texture2D
        fun createFromFile(file: String, context: GraphicsContext, type: TextureType): Texture2D
    }

}

package org.etieskrill.engine.graphics.texture

import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc
import io.github.etieskrill.injection.extension.shader.TextureCubeMap as DslTextureCubeMap

class TextureCubeMap(
    override val size: Vector2ic,
    type: TextureType,
    internal val buffer: List<ByteArray>? = null,
    internal val file: String? = null,
    format: TextureFormat,
    minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
    magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    borderColour: Vector4fc = Vector4f(0f)
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour), DslTextureCubeMap,
    FrameBufferAttachment { //TODO does FrameBufferAttachment really make sense?

    companion object {
        const val NUM_SIDES: Int = 6

        fun createBlank(size: Vector2ic, format: TextureFormat): TextureCubeMap =
            TextureCubeMap(size, TextureType.UNKNOWN, format = format)

        fun createFromBuffer(size: Vector2ic, buffer: List<ByteArray>, format: TextureFormat): TextureCubeMap =
            TextureCubeMap(size, TextureType.UNKNOWN, buffer, format = format)

        fun createFromFile(file: String, type: TextureType): TextureCubeMap {
            val (size, format, _) = readCubeMapFiles(file, type) //TODO replace header-only reader
            return TextureCubeMap(size, type, file = file, format = format)
        }

        //?
//        fun createSkybox(file: String, context: GraphicsContext): TextureCubeMap {
//            val (size, format, textureData) = readCubeMapFiles(file, TextureType.DIFFUSE)
//            return TextureCubeMap(
//                context, size, textureData, format, TextureType.DIFFUSE,
//                TextureMinFilter.LINEAR, TextureMagFilter.LINEAR, TextureWrapping.CLAMP_TO_EDGE
//            )
//        }
    }

}

internal expect class TextureCubeMapInstance : TextureInstance<TextureCubeMap>, FrameBufferAttachmentInstance

internal expect fun readCubeMapFiles(file: String, type: TextureType): Triple<Vector2ic, TextureFormat, List<ByteArray>>

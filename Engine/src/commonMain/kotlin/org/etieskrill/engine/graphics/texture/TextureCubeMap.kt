package org.etieskrill.engine.graphics.texture

import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import io.github.etieskrill.injection.extension.shader.TextureCubeMap as DslTextureCubeMap

class TextureCubeMap(
    override val size: Vec2ic,
    type: TextureType,
    internal val buffer: List<ByteArray>? = null,
    internal val file: String? = null,
    format: TextureFormat,
    minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
    magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    borderColour: Vec4c = Vec4(0f),
    rowAlignment: TextureRowAlignment = TextureRowAlignment.WORD
) : Texture(type, format, minFilter, magFilter, wrapping, borderColour, rowAlignment), DslTextureCubeMap,
    FrameBufferAttachment { //TODO does FrameBufferAttachment really make sense?

    companion object {
        const val NUM_SIDES: Int = 6

        fun createBlank(size: Vec2ic, format: TextureFormat): TextureCubeMap =
            TextureCubeMap(size, TextureType.UNKNOWN, format = format)

        fun createFromBuffer(size: Vec2ic, buffer: List<ByteArray>, format: TextureFormat): TextureCubeMap =
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

internal expect fun readCubeMapFiles(file: String, type: TextureType): Triple<Vec2ic, TextureFormat, List<ByteArray>>

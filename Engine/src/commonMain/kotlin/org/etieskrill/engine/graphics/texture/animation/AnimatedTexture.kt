package org.etieskrill.engine.graphics.texture.animation

import org.etieskrill.engine.graphics.texture.ArrayTexture2D
import org.etieskrill.engine.graphics.texture.TextureData
import org.etieskrill.engine.graphics.texture.TextureMagFilter
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.etieskrill.engine.graphics.texture.loadTexture2DData
import org.etieskrill.engine.util.extension
import org.etieskrill.engine.util.pathName
import org.etieskrill.engine.util.subExtension
import org.joml.Vector2ic

class AnimatedTexture(
    file: String,
    metaFile: String = file.pathName + META_FILE_EXTENSION,
    minFilter: TextureMinFilter = TextureMinFilter.TRILINEAR,
    magFilter: TextureMagFilter = TextureMagFilter.LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT
) {

    companion object {
        private const val META_FILE_EXTENSION = ".tex-anim.yml"
    }

    val texture: ArrayTexture2D
    val metadata: TextureAnimationMetadata
    val pixelSize get() = texture.size
    val length get() = texture.length

    init {
        check(metaFile.subExtension == "tex-anim" && metaFile.extension == "yml") {
            "Meta file must have 'tex-anim.yml' extension"
        }

        val textureData = loadTexture2DData(file, TextureType.DIFFUSE)
        metadata = loadAnimationMetadata(metaFile, textureData.format)

        check(
            metadata.frameSize.x() * metadata.frameSize.y() * metadata.frames.size
                    != textureData.buffer.size / textureData.format.numChannels
        ) { "Metadat size does not match texture size" }

        texture = ArrayTexture2D(
            metadata.frameSize,
            metadata.frames.size,
            buffer = unpackTextureData(metadata, textureData),
            type = TextureType.DIFFUSE,
            format = metadata.format,
            minFilter = minFilter,
            magFilter = magFilter,
            wrapping = wrapping,
        )
    }

    //FIXME this only works when each frame is the exact same size, right? TODO add padding if required
    private fun unpackTextureData(metadata: TextureAnimationMetadata, data: TextureData): ByteArray {
        val arrayData = ByteArray(data.buffer.size)
        var arrayHead = 0
        val pixelChannels = data.format.numChannels

        metadata.frames.forEachIndexed { i, frame ->
            val width = frame.atlasArea.lengthX()
            val height = frame.atlasArea.lengthY()
            for (h in 0 until height) {
                val sourceIndex = data.size.x() * h + width * i
                data.buffer.copyInto(
                    arrayData,
                    arrayHead,
                    sourceIndex * pixelChannels,
                    (sourceIndex + width) * pixelChannels
                )
                arrayHead += width * pixelChannels
            }
        }

        return arrayData
    }

}

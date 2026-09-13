package org.etieskrill.engine.graphics.texture.animation

import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.util.readResource
import org.joml.Vector2i
import org.joml.primitives.Rectanglei
import org.yaml.snakeyaml.Yaml

private const val MILLIS_TO_SECONDS = 0.001f

data class YamlTextureAnimationMetadata(
    val `$schema`: String,
    val resources: Map<String, YamlTextureAnimationResource>,
    val frames: Map<String, YamlTextureAnimationFrame>
)

data class YamlTextureAnimationResource(val file: String)

data class YamlTextureAnimationFrame(
    val x: Int?,
    val y: Int?,
    val w: Int?,
    val h: Int?,
    val duration: Float?
)

internal fun loadAnimationMetadata(metaFile: String, format: TextureFormat): TextureAnimationMetadata {
    val yaml = Yaml()
    val yamlMetaData = yaml.loadAs(readResource(metaFile).inputStream(), YamlTextureAnimationMetadata::class.java)

    check(yamlMetaData.resources["texture"] != null) { "Animation resource cannot be null" }

    val defaultFrame = yamlMetaData.frames["default"] ?: error("Animated texture must have a default frame")
    var maxX = 0
    var maxY = 0
    var currentTimeMillis = 0f

    val frames = yamlMetaData.frames
        .filterNot { it.key == "default" }
        .values.map { frame ->
            val parsedFrame = parseFrame(frame, defaultFrame, currentTimeMillis)
            maxX = maxOf(maxX, parsedFrame.atlasArea.lengthX())
            maxY = maxOf(maxY, parsedFrame.atlasArea.lengthY())
            currentTimeMillis += frame.duration ?: defaultFrame.duration!!
            parsedFrame
        }

    return TextureAnimationMetadata(
        yamlMetaData.resources["texture"]!!.file,
        Vector2i(maxX, maxY),
        format,
        frames,
        currentTimeMillis * MILLIS_TO_SECONDS
    )
}

private fun parseFrame(
    frame: YamlTextureAnimationFrame?,
    defaultFrame: YamlTextureAnimationFrame,
    currentTimeMillis: Float
): TextureAnimationFrame {
    frame ?: return parseDefaultFrame(defaultFrame, currentTimeMillis)

    val x = frame.x ?: defaultFrame.x!!
    val y = frame.y ?: defaultFrame.y!!
    val newFrame = TextureAnimationFrame(
        Rectanglei(x, y, x + (frame.w ?: defaultFrame.w!!), y + (frame.h ?: defaultFrame.h!!)),
        currentTimeMillis * MILLIS_TO_SECONDS
    )

    return newFrame
}

private fun parseDefaultFrame(yamlFrame: YamlTextureAnimationFrame, currentTimeMillis: Float) =
    TextureAnimationFrame(
        Rectanglei(yamlFrame.x!!, yamlFrame.y!!, yamlFrame.x + yamlFrame.w!!, yamlFrame.y + yamlFrame.h!!),
        currentTimeMillis * MILLIS_TO_SECONDS
    )

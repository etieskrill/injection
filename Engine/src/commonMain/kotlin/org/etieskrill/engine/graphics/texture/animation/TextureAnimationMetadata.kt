package org.etieskrill.engine.graphics.texture.animation

import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.graphics.texture.TextureFormat

data class TextureAnimationMetadata(
    val textureFile: String,
    val frameSize: Vec2ic,
    val format: TextureFormat,
    val frames: List<TextureAnimationFrame>,
    val duration: Float
)

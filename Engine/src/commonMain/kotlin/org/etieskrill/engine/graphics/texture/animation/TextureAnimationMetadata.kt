package org.etieskrill.engine.graphics.texture.animation

import org.etieskrill.engine.graphics.texture.TextureFormat
import org.joml.Vector2ic

data class TextureAnimationMetadata(
    val textureFile: String,
    val frameSize: Vector2ic,
    val format: TextureFormat,
    val frames: List<TextureAnimationFrame>,
    val duration: Float
)

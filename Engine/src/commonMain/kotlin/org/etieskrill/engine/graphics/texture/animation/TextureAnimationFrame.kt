package org.etieskrill.engine.graphics.texture.animation

import org.joml.primitives.Rectanglei

data class TextureAnimationFrame(
    val atlasArea: Rectanglei,
    val time: Float
)

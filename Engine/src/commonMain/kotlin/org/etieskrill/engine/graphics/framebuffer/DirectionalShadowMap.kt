package org.etieskrill.engine.graphics.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.joml.Vector2ic
import org.joml.Vector4f

class DirectionalShadowMap(
    size: Vector2ic
) : ShadowMap<Texture2D>(
    size,
    Texture2D(
        size,
        TextureType.SHADOW,
        format = TextureFormat.DEPTH,
        minFilter = TextureMinFilter.LINEAR,
        wrapping = TextureWrapping.CLAMP_TO_BORDER,
        borderColour = Vector4f(1f)
    )
), Texture2DShadow

internal expect class DirectionalShadowMapInstance : ShadowMapInstance<Texture2D>

package org.etieskrill.engine.graphics.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping

class DirectionalShadowMap(
    size: Vec2ic
) : ShadowMap<Texture2D>(
    size,
    Texture2D(
        size,
        TextureType.SHADOW,
        format = TextureFormat.DEPTH,
        minFilter = TextureMinFilter.LINEAR,
        wrapping = TextureWrapping.CLAMP_TO_BORDER,
        borderColour = Vec4(1f)
    )
), Texture2DShadow

internal expect class DirectionalShadowMapInstance : ShadowMapInstance<Texture2D>

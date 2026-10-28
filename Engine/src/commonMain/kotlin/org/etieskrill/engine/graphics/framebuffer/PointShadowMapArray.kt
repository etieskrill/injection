package org.etieskrill.engine.graphics.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DArrayShadow
import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.graphics.texture.TextureCubeMapArray
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping

class PointShadowMapArray(
    size: Vec2ic,
    val length: Int
) : ShadowMap<TextureCubeMapArray>(
    size,
    TextureCubeMapArray(
        size,
        length,
        TextureType.SHADOW,
        format = TextureFormat.DEPTH,
        minFilter = TextureMinFilter.LINEAR,
        wrapping = TextureWrapping.CLAMP_TO_EDGE
    )
), Texture2DArrayShadow

internal expect class PointShadowMapArrayInstance : ShadowMapInstance<TextureCubeMapArray>

package org.etieskrill.engine.graphics.framebuffer

import io.github.etieskrill.injection.extension.shader.TextureCubeMapShadow
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.joml.Vector2ic

class PointShadowMap(
    size: Vector2ic
) : ShadowMap<TextureCubeMap>(
    size,
    TextureCubeMap(
        size,
        TextureType.SHADOW,
        format = TextureFormat.DEPTH,
        minFilter = TextureMinFilter.LINEAR,
        wrapping = TextureWrapping.CLAMP_TO_EDGE
    )
), TextureCubeMapShadow

internal expect class PointShadowMapInstance : ShadowMapInstance<TextureCubeMap>

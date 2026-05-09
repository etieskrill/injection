package org.etieskrill.engine.graphics.gl.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Vector2ic
import org.joml.Vector4f

class DirectionalShadowMap(
    size: Vector2ic
) : ShadowMap<Texture2D>(
    size,
    Texture2D.BlankBuilder(size)
        .setFormat(AbstractTexture.Format.DEPTH)
        .setType(AbstractTexture.Type.SHADOW)
        .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
        .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_BORDER)
        .setBorderColour(Vector4f(1f))
        .build()
), Texture2DShadow

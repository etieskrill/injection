package org.etieskrill.engine.graphics.gl.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Vector2ic
import org.joml.Vector4f

class DirectionalShadowMap(
    context: GraphicsContext,
    size: Vector2ic
) : ShadowMap<Texture2D>(
    context,
    size,
    Texture2D.BlankBuilder(size)
        .setFormat(Texture.Format.DEPTH)
        .setType(Texture.Type.SHADOW)
        .setMipMapping(Texture.MinFilter.LINEAR, Texture.MagFilter.LINEAR)
        .setWrapping(Texture.Wrapping.CLAMP_TO_BORDER)
        .setBorderColour(Vector4f(1f))
        .build()
), Texture2DShadow

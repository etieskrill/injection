package org.etieskrill.engine.graphics.gl.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureFormat
import org.etieskrill.engine.graphics.texture.TextureMagFilter
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.joml.Vector2ic
import org.joml.Vector4f

class DirectionalShadowMap(
    context: GraphicsContext,
    size: Vector2ic
) : ShadowMap<Texture2D>(
    context,
    size,
    Texture2D(
        size,
        TextureType.SHADOW,
        format = TextureFormat.DEPTH,
        minFilter = TextureMinFilter.LINEAR,
        magFilter = TextureMagFilter.LINEAR,
        wrapping = TextureWrapping.CLAMP_TO_BORDER,
        borderColour = Vector4f(1f)
    )
), Texture2DShadow

package org.etieskrill.engine.graphics.gl.framebuffer

import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.graphics.texture.CubeMapTexture
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector2ic
import org.joml.Vector4f

class PointShadowMap(
    context: GraphicsContext,
    size: Vector2ic
) : ShadowMap<CubeMapTexture>(
    context,
    size,
    CubeMapTexture.MemoryBuilder(size)
        .setFormat(Texture.Format.DEPTH)
        .setType(Texture.Type.SHADOW)
        .setMipMapping(Texture.MinFilter.LINEAR, Texture.MagFilter.LINEAR)
        .setWrapping(Texture.Wrapping.CLAMP_TO_EDGE)
        .setBorderColour(Vector4f(1f))
        .build()
), Texture2DShadow {

    private val cachedMatrices = Array(6) { Matrix4f() }

    fun calculateCombinedMatrices(near: Float, far: Float, light: PointLight): Array<out Matrix4fc> =
        getCombinedMatrices(size, near, far, light, cachedMatrices)

}

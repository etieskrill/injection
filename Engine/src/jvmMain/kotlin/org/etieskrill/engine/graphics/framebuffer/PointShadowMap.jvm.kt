package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureCubeMapInstance
import org.joml.Matrix4f
import org.joml.Matrix4fc

internal actual class PointShadowMapInstance(
    descriptor: PointShadowMap,
    context: GraphicsContext
) : ShadowMapInstance<TextureCubeMap>(descriptor, context) {

    private val cachedMatrices = Array(6) { Matrix4f() }

    fun calculateCombinedMatrices(near: Float, far: Float, light: PointLight): Array<out Matrix4fc> =
        getCombinedMatrices(descriptor.size, near, far, light, cachedMatrices)

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) =
        (context.getTexture(descriptor.texture) as TextureCubeMapInstance).attach(frameBuffer, type)

}

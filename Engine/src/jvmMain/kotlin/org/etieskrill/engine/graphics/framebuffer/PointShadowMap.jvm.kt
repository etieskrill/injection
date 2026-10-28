package org.etieskrill.engine.graphics.framebuffer

import net.bifreus.kml.Mat4
import net.bifreus.kml.Mat4c
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureCubeMapInstance

internal actual class PointShadowMapInstance(
    descriptor: PointShadowMap,
    context: GraphicsContext
) : ShadowMapInstance<TextureCubeMap>(descriptor, context) {

    private val cachedMatrices = Array(6) { Mat4() }

    fun calculateCombinedMatrices(near: Float, far: Float, light: PointLight): Array<out Mat4c> =
        getCombinedMatrices(descriptor.size, near, far, light, cachedMatrices)

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) =
        (context.getTexture(descriptor.texture) as TextureCubeMapInstance).attach(frameBuffer, type)

}

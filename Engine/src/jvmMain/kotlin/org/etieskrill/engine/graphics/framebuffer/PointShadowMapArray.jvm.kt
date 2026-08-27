package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.texture.TextureCubeMapArray
import org.etieskrill.engine.graphics.texture.TextureCubeMapArrayInstance

internal actual class PointShadowMapArrayInstance(
    descriptor: PointShadowMapArray,
    context: GraphicsContext
) : ShadowMapInstance<TextureCubeMapArray>(descriptor, context) {

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) =
        (context.getTexture(descriptor.texture) as TextureCubeMapArrayInstance).attach(frameBuffer, type)

}

package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.Texture2DInstance

internal actual class DirectionalShadowMapInstance(
    descriptor: DirectionalShadowMap,
    context: GraphicsContext
) : ShadowMapInstance<Texture2D>(descriptor, context) {

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) =
        (context.getTexture(descriptor.texture) as Texture2DInstance).attach(frameBuffer, type)

}

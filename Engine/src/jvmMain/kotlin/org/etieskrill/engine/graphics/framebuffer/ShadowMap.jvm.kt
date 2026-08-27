package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.texture.Texture
import org.lwjgl.opengl.GL11C.GL_LEQUAL
import org.lwjgl.opengl.GL11C.glTexParameteri
import org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_FUNC
import org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE
import org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE

internal actual abstract class ShadowMapInstance<T>(
    actual override val descriptor: ShadowMap<T>,
    context: GraphicsContext
) : FrameBufferInstance(descriptor, context), FrameBufferAttachmentInstance,
    Disposable where T : Texture, T : FrameBufferAttachment {

    init {
        context.withContext {
            val texture = context.getTexture(descriptor.texture)
            texture.bind(0)
            glTexParameteri(texture.glTarget, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE)
            glTexParameteri(texture.glTarget, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL)
        }
    }

    override fun dispose() {
        TODO("maybe introduce a reference counter and autodispose instances instead of this explicit stuff")
    }

}

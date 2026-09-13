package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.RenderBufferType.*
import org.etieskrill.engine.graphics.gl.GLUtils
import org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL30C.*

internal actual class RenderBufferInstance(
    actual val descriptor: RenderBuffer,
    actual val context: GraphicsContext
) : FrameBufferAttachmentInstance, Disposable {

    init {
        context.withContext { GLUtils.clearError() }
    }

    private val id: Int = context.withContext { glGenRenderbuffers() }

    init {
        context.withContext {
            bind()

            glRenderbufferStorage(
                GL_RENDERBUFFER,
                descriptor.type.gl,
                descriptor.size.x(), descriptor.size.y()
            )

            GLUtils.checkErrorThrowing("Error during renderbuffer creation")
        }
    }

    fun bind() = context.withContext { glBindRenderbuffer(GL_RENDERBUFFER, id) }
    fun unbind() = context.withContext { glBindRenderbuffer(GL_RENDERBUFFER, 0) }

    actual override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) =
        context.withContext {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, type.gl, GL_RENDERBUFFER, id)
        }

    override fun dispose() = context.withContext { glDeleteRenderbuffers(id) }

}

internal val RenderBufferType.gl: Int
    get() = when (this) {
        COLOUR -> GL_RGBA8
        DEPTH -> GL_DEPTH_COMPONENT
        DEPTH_HIGHP -> GL_DEPTH_COMPONENT32F
        STENCIL -> GL_STENCIL_ATTACHMENT
        DEPTH_STENCIL -> GL_DEPTH24_STENCIL8
        DEPTH_HIGHP_STENCIL -> GL_DEPTH32F_STENCIL8
    }

package org.etieskrill.engine.graphics.gl.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.GLUtils
import org.joml.Vector2ic
import org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL30C.*

class RenderBuffer(
    override val size: Vector2ic,
    val type: Type
) : Disposable, FrameBufferAttachment {

    private val id: Int

    init {
        GLUtils.clearError()

        id = glGenRenderbuffers()
        bind()

        glRenderbufferStorage(GL_RENDERBUFFER, type.glType, size.x(), size.y())

        GLUtils.checkErrorThrowing("Error during renderbuffer creation")
    }

    enum class Type {
        COLOUR,
        DEPTH,
        DEPTH_HIGHP,
        STENCIL,
        DEPTH_STENCIL,
        DEPTH_HIGHP_STENCIL;

        val glType
            get() = when (this) {
                COLOUR -> GL_RGBA8
                DEPTH -> GL_DEPTH_COMPONENT
                DEPTH_HIGHP -> GL_DEPTH_COMPONENT32F
                STENCIL -> GL_STENCIL_ATTACHMENT
                DEPTH_STENCIL -> GL_DEPTH24_STENCIL8
                DEPTH_HIGHP_STENCIL -> GL_DEPTH32F_STENCIL8
            }
    }

    fun bind() = glBindRenderbuffer(GL_RENDERBUFFER, id)
    fun unbind() = glBindRenderbuffer(GL_RENDERBUFFER, 0)

    override fun attach(type: FrameBufferAttachmentType) =
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, type.glAttachmentType, GL_RENDERBUFFER, id)

    override fun dispose() = glDeleteRenderbuffers(id)

}

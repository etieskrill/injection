package org.etieskrill.engine.graphics.gl.framebuffer

import org.etieskrill.engine.common.Disposable
import org.joml.Vector2ic
import org.lwjgl.opengl.GL30C.*

interface FrameBufferAttachment : Disposable {

    val size: Vector2ic

    /**
     * Calls for the {@code attachment} to attach itself to the currently bound framebuffer, which is guaranteed to be
     * valid.
     *
     * @param type the binding point in the framebuffer which the attachment must bind itself to
     */
    fun attach(type: FrameBufferAttachmentType)

}

enum class FrameBufferAttachmentType(val glAttachmentType: Int) {
    COLOUR0(GL_COLOR_ATTACHMENT0),
    COLOUR1(GL_COLOR_ATTACHMENT1),
    COLOUR2(GL_COLOR_ATTACHMENT2),
    COLOUR3(GL_COLOR_ATTACHMENT3),
    COLOUR31(GL_COLOR_ATTACHMENT31),
    DEPTH(GL_DEPTH_ATTACHMENT),
    STENCIL(GL_STENCIL_ATTACHMENT),
    DEPTH_STENCIL(GL_DEPTH_STENCIL_ATTACHMENT)
}

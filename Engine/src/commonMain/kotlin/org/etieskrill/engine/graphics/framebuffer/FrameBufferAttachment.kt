package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.joml.Vector2ic
import org.lwjgl.opengl.GL30C.*

interface FrameBufferAttachment : Disposable {

    val size: Vector2ic

    /**
     * Calls for this attachment to attach itself to [frameBuffer] as a [type].
     *
     * @param frameBuffer the framebuffer to attach to
     * @param type the binding point the attachment must bind itself to
     */
    fun attach(frameBuffer: FrameBuffer, type: FrameBufferAttachmentType)

}

enum class FrameBufferAttachmentType {
    COLOUR0, COLOUR1, COLOUR2, COLOUR3, COLOUR31,
    DEPTH, STENCIL, DEPTH_STENCIL
}

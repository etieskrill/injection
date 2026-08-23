package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.joml.Vector2ic

interface FrameBufferAttachment {

    val size: Vector2ic

}

enum class FrameBufferAttachmentType {
    COLOUR0, COLOUR1, COLOUR2, COLOUR3, COLOUR31,
    DEPTH, STENCIL, DEPTH_STENCIL
}

interface FrameBufferAttachmentInstance : Disposable {

    /**
     * Calls for this attachment instance to attach itself to [FrameBufferInstance] as a [type].
     *
     * @param type the binding point the attachment must bind itself to
     */
    fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType)

}

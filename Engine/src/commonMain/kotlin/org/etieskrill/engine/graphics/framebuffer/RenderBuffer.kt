package org.etieskrill.engine.graphics.framebuffer

import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext

class RenderBuffer(
    override val size: Vec2ic,
    val type: RenderBufferType
) : FrameBufferAttachment

enum class RenderBufferType {
    COLOUR, DEPTH, DEPTH_HIGHP, STENCIL, DEPTH_STENCIL, DEPTH_HIGHP_STENCIL
}

internal expect class RenderBufferInstance : FrameBufferAttachmentInstance, Disposable {
    val descriptor: RenderBuffer
    val context: GraphicsContext

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType)
}

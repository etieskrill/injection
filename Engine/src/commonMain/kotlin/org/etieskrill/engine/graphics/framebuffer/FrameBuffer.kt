package org.etieskrill.engine.graphics.framebuffer

import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType.COLOUR0
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType.DEPTH_STENCIL
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer as DslFrameBuffer

open class FrameBuffer(
    override val size: Vec2ic,
    val attachments: Map<FrameBufferAttachmentType, FrameBufferAttachment>
) : DslFrameBuffer {

    var clearColour: Vec4 = Vec4(0f)
        set(value) {
            field(value)
        }

    internal var version: Long = 0L

    init {
        attachments.forEach { (type, attachment) ->
            require(attachment.size == size) {
                "Framebuffer attachment size for ${attachment::class.simpleName} bound to slot $type (${
                    attachment.size
                }) does not match framebuffer size ($size)"
            }
        }
    }

    fun clear() {
        version++
    }

    companion object {
        fun getStandard(size: Vec2ic): FrameBuffer = FrameBuffer(
            size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBufferType.COLOUR),
                DEPTH_STENCIL to RenderBuffer(size, RenderBufferType.DEPTH_STENCIL),
            )
        )

        fun getColour(size: Vec2ic): FrameBuffer = FrameBuffer(
            size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBufferType.COLOUR),
            )
        )
    }

}

expect open class FrameBufferInstance : Disposable {
    open val descriptor: FrameBuffer
    val context: GraphicsContext

    internal var version: Long

    override fun dispose()
}

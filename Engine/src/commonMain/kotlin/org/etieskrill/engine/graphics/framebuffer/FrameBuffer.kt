package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType.COLOUR0
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType.DEPTH_STENCIL
import org.joml.Vector2ic
import org.joml.Vector4f
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer as DslFrameBuffer

open class FrameBuffer(
    override val size: Vector2ic,
    val attachments: Map<FrameBufferAttachmentType, FrameBufferAttachment>
) : DslFrameBuffer {

    var clearColour: Vector4f = Vector4f(0f)
        set(value) {
            field.set(value)
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
        fun getStandard(size: Vector2ic): FrameBuffer = FrameBuffer(
            size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBufferType.COLOUR),
                DEPTH_STENCIL to RenderBuffer(size, RenderBufferType.DEPTH_STENCIL),
            )
        )

        fun getColour(size: Vector2ic): FrameBuffer = FrameBuffer(
            size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBufferType.COLOUR),
            )
        )
    }

}

expect class FrameBufferInstance : Disposable {
    val descriptor: FrameBuffer
    val context: GraphicsContext

    internal var version: Long

    override fun dispose()
}

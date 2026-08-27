package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType.*
import org.etieskrill.engine.graphics.gl.GLUtils
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import kotlin.properties.Delegates.notNull

@OptIn(ExperimentalStdlibApi::class)
actual open class FrameBufferInstance internal constructor(
    actual open val descriptor: FrameBuffer,
    actual val context: GraphicsContext,
) : Disposable {

    internal actual var version: Long = 0L

    internal open var id: Int by notNull()

    protected var glBufferClearMask: Int by notNull()
    protected var glColourDrawBuffers: IntArray by notNull()

    init {
        init()
    }

    protected open fun init() = context.withContext {
        GLUtils.clearError()

        id = GL30C.glGenFramebuffers()

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, id)
        descriptor.attachments.forEach { (type, attachment) ->
            context.getFrameBufferAttachment(attachment)
                .attach(this, type)

            //FIXME this could be anything from improperly sized attachments to any other attribute not matching
            // exactly, so either exercise VERY strict validation before/during/after attaching, or find a way to
            // get any sort of logs
            if (GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                error("Incomplete framebuffer attachment for $type: $attachment")
            }
        }

        when (val ret = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER)) {
            GL30C.GL_FRAMEBUFFER_COMPLETE -> null
            GL30C.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "an incomplete attachment"
            GL30C.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "a missing attachment"
            GL30C.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "an incomplete draw buffer"
            GL30C.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "an incomplete read buffer"
            GL30C.GL_FRAMEBUFFER_UNSUPPORTED -> "an unsupported framebuffer"
            GL30C.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "an incomplete multisample buffer"
            GL30C.GL_FRAMEBUFFER_UNDEFINED -> "an undefined framebuffer"
            else -> "an unknown framebuffer error: 0x${ret.toHexString()}"
        }?.let { throw FrameBufferCreationException("Framebuffer was not successfully completed due to $it") }

        GLUtils.checkErrorThrowing("Error during framebuffer creation")

        var glBufferClearMask = 0
        if (descriptor.attachments.keys.any { it in listOf(COLOUR0, COLOUR1, COLOUR2, COLOUR3, COLOUR31) }) {
            glBufferClearMask = glBufferClearMask or GL11C.GL_COLOR_BUFFER_BIT
        }
        if (descriptor.attachments.keys.any { it in listOf(DEPTH, DEPTH_STENCIL) }) {
            glBufferClearMask = glBufferClearMask or GL11C.GL_DEPTH_BUFFER_BIT
        }
        if (descriptor.attachments.keys.any { it in listOf(STENCIL, DEPTH_STENCIL) }) {
            glBufferClearMask = glBufferClearMask or GL11C.GL_STENCIL_BUFFER_BIT
        }
        this.glBufferClearMask = glBufferClearMask

        glColourDrawBuffers = descriptor.attachments.keys.map { it.gl }
            .filter { it in COLOUR0.gl..COLOUR31.gl }
            .sorted()
            .toIntArray()
    }

    enum class Binding { READ, WRITE, BOTH }

    open fun bind() = bind(Binding.BOTH)

    fun bind(binding: Binding) = context.withContext {
        if (context.activeFramebuffer != this) {
            GL30C.glBindFramebuffer(
                when (binding) {
                    Binding.READ -> GL30C.GL_READ_FRAMEBUFFER
                    Binding.WRITE -> GL30C.GL_DRAW_FRAMEBUFFER
                    Binding.BOTH -> GL30C.GL_FRAMEBUFFER
                }, id
            )
            GL20C.glDrawBuffers(glColourDrawBuffers)
            GL11C.glViewport(0, 0, descriptor.size.x(), descriptor.size.y())
            context.activeFramebuffer = this
        }

        if (version < descriptor.version) {
            descriptor.clearColour.apply { GL11C.glClearColor(x, y, z, w) }
            if ((glBufferClearMask and GL11C.GL_DEPTH_BUFFER_BIT) != 0) GL11C.glDepthMask(true)
            if ((glBufferClearMask and GL11C.GL_STENCIL_BUFFER_BIT) != 0) GL11C.glStencilMask(0xFF) //TODO can stencil buffer be anything other than one byte in size?
            GL11C.glClear(glBufferClearMask)

            version = descriptor.version
        }
    }

    /**
     * Unbinds the currently bound framebuffer, which is identical to binding the
     * [context's screen buffer](org.etieskrill.engine.window.Window.screenBuffer).
     */
    fun unbind() = context.screenBuffer.bind()

    actual override fun dispose() = context.withContext {
        unbind()
        GL30C.glDeleteFramebuffers(id)
        descriptor.attachments.values.forEach { context.getFrameBufferAttachment(it).dispose() }
    }

}

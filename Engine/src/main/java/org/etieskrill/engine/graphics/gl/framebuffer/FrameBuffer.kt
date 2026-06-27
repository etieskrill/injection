package org.etieskrill.engine.graphics.gl.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachmentType.*
import org.joml.Vector2ic
import org.joml.Vector4f
import org.lwjgl.opengl.GL20C.glDrawBuffers
import org.lwjgl.opengl.GL30C.glDeleteFramebuffers
import org.lwjgl.opengl.GL33C.*
import kotlin.properties.Delegates.notNull
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer as DslFrameBuffer

@OptIn(ExperimentalStdlibApi::class)
open class FrameBuffer internal constructor(
    override val size: Vector2ic,
    val attachments: Map<FrameBufferAttachmentType, FrameBufferAttachment>,
    id: Int
) : DslFrameBuffer, Disposable {

    var clearColour: Vector4f = Vector4f(0f)
        set(value) {
            field.set(value)
        }

    protected val id = id

    protected var glBufferClearMask: Int by notNull()
    protected var glColourDrawBuffers: IntArray by notNull()

    constructor(size: Vector2ic, attachments: Map<FrameBufferAttachmentType, FrameBufferAttachment>)
            : this(size, attachments, glGenFramebuffers())

    init {
        init()
    }

    internal open fun init() {
        GLUtils.clearError()

        glBindFramebuffer(GL_FRAMEBUFFER, id)
        attachments.forEach { (type, attachment) ->
            require(attachment.size == size) {
                "Framebuffer attachment size for ${attachment::class.simpleName} bound to slot $type (${
                    attachment.size
                }) does not match framebuffer size ($size)"
            }

            attachment.attach(type)

            //FIXME this could be anything from improperly sized attachments to any other attribute not matching
            // exactly, so either exercise VERY strict validation before/during/after attaching, or find a way to
            // get any sort of logs
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                error("Incomplete framebuffer attachment for $type: $attachment")
            }
        }

        when (val ret = glCheckFramebufferStatus(GL_FRAMEBUFFER)) {
            GL_FRAMEBUFFER_COMPLETE -> null
            GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "an incomplete attachment"
            GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "a missing attachment"
            GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "an incomplete draw buffer"
            GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "an incomplete read buffer"
            GL_FRAMEBUFFER_UNSUPPORTED -> "an unsupported framebuffer"
            GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "an incomplete multisample buffer"
            GL_FRAMEBUFFER_UNDEFINED -> "an undefined framebuffer"
            else -> "an unknown framebuffer error: 0x${ret.toHexString()}"
        }?.let { throw FrameBufferCreationException("Framebuffer was not successfully completed due to $it") }

        GLUtils.checkErrorThrowing("Error during framebuffer creation")

        var glBufferClearMask = 0
        if (attachments.keys.any { it in listOf(COLOUR0, COLOUR1, COLOUR2, COLOUR3, COLOUR31) }) {
            glBufferClearMask = glBufferClearMask or GL_COLOR_BUFFER_BIT
        }
        if (attachments.keys.any { it in listOf(DEPTH, DEPTH_STENCIL) }) {
            glBufferClearMask = glBufferClearMask or GL_DEPTH_BUFFER_BIT
        }
        if (attachments.keys.any { it in listOf(STENCIL, DEPTH_STENCIL) }) {
            glBufferClearMask = glBufferClearMask or GL_STENCIL_BUFFER_BIT
        }
        this.glBufferClearMask = glBufferClearMask

        glColourDrawBuffers = attachments.keys.map { it.glAttachmentType }
            .filter { it in COLOUR0.glAttachmentType..COLOUR31.glAttachmentType }
            .sorted()
            .toIntArray()
    }

    companion object {
        fun getStandard(size: Vector2ic) = FrameBuffer(
            size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBuffer.Type.COLOUR),
                DEPTH_STENCIL to RenderBuffer(size, RenderBuffer.Type.DEPTH_STENCIL),
            )
        )

        fun getColour(size: Vector2ic) = FrameBuffer(
            size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBuffer.Type.COLOUR)
            )
        )
    }

    enum class Binding { READ, WRITE, BOTH }

    override fun bind() {
        bind(Binding.BOTH)
    }

    fun bind(binding: Binding) {
        //TODO maybe move context check here?
        glBindFramebuffer(
            when (binding) {
                Binding.READ -> GL_READ_FRAMEBUFFER
                Binding.WRITE -> GL_DRAW_FRAMEBUFFER
                Binding.BOTH -> GL_FRAMEBUFFER
            }, id
        )
        glDrawBuffers(glColourDrawBuffers)
        glViewport(0, 0, size.x(), size.y())
    }

    /**
     * Unbinds the currently bound framebuffer, which is identical to binding the
     * [window's screen buffer](org.etieskrill.engine.window.Window.screenBuffer), except that it does <b>NOT</b>
     * reset the viewport size to the screen buffer's size.
     */
    fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    fun clear() {
        bind()
        glClearColor(clearColour.x, clearColour.y, clearColour.z, clearColour.w)
        if ((glBufferClearMask and GL_DEPTH_BUFFER_BIT) != 0) glDepthMask(true)
        if ((glBufferClearMask and GL_STENCIL_BUFFER_BIT) != 0) glStencilMask(0xFF) //TODO can stencil buffer be anything other than one byte in size?
        glClear(glBufferClearMask)
        unbind()
    }

    override fun dispose() {
        glDeleteFramebuffers(id)
        attachments.values.forEach(Disposable::dispose)
    }

}

package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.GraphicsContextBound
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachmentType
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachmentType.COLOUR0
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachmentType.DEPTH_STENCIL
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferCreationException
import org.etieskrill.engine.graphics.gl.framebuffer.RenderBuffer
import org.joml.Vector2ic
import org.joml.Vector4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import kotlin.properties.Delegates
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer as DslFrameBuffer

@OptIn(ExperimentalStdlibApi::class)
actual open class FrameBuffer internal constructor(
    override val context: GraphicsContext,
    override val size: Vector2ic,
    val attachments: Map<FrameBufferAttachmentType, FrameBufferAttachment>,
    id: Int
) : DslFrameBuffer, GraphicsContextBound, Disposable {

    actual var clearColour: Vector4f = Vector4f(0f)
        set(value) {
            field.set(value)
        }

    protected val id = id

    protected var glBufferClearMask: Int by Delegates.notNull()
    protected var glColourDrawBuffers: IntArray by Delegates.notNull()

    constructor(
        context: GraphicsContext,
        size: Vector2ic,
        attachments: Map<FrameBufferAttachmentType, FrameBufferAttachment>
    ) : this(context, size, attachments, GL30C.glGenFramebuffers())

    actual companion object {
        actual fun getStandard(context: GraphicsContext, size: Vector2ic) = FrameBuffer(
            context, size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBuffer.Type.COLOUR),
                DEPTH_STENCIL to RenderBuffer(size, RenderBuffer.Type.DEPTH_STENCIL),
            )
        )

        actual fun getColour(context: GraphicsContext, size: Vector2ic) = FrameBuffer(
            context, size, mapOf(
                COLOUR0 to RenderBuffer(size, RenderBuffer.Type.COLOUR)
            )
        )
    }

    init {
        init()
    }

    internal open fun init() = context.withContext {
        GLUtils.clearError()

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, id)
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
        if (attachments.keys.any {
                it in listOf(
                    COLOUR0,
                    FrameBufferAttachmentType.COLOUR1,
                    FrameBufferAttachmentType.COLOUR2,
                    FrameBufferAttachmentType.COLOUR3,
                    FrameBufferAttachmentType.COLOUR31
                )
            }) {
            glBufferClearMask = glBufferClearMask or GL11C.GL_COLOR_BUFFER_BIT
        }
        if (attachments.keys.any {
                it in listOf(
                    FrameBufferAttachmentType.DEPTH,
                    DEPTH_STENCIL
                )
            }) {
            glBufferClearMask = glBufferClearMask or GL11C.GL_DEPTH_BUFFER_BIT
        }
        if (attachments.keys.any {
                it in listOf(
                    FrameBufferAttachmentType.STENCIL,
                    DEPTH_STENCIL
                )
            }) {
            glBufferClearMask = glBufferClearMask or GL11C.GL_STENCIL_BUFFER_BIT
        }
        this@FrameBuffer.glBufferClearMask = glBufferClearMask

        glColourDrawBuffers = attachments.keys.map { it.glAttachmentType }
            .filter { it in COLOUR0.glAttachmentType..FrameBufferAttachmentType.COLOUR31.glAttachmentType }
            .sorted()
            .toIntArray()
    }


    enum class Binding { READ, WRITE, BOTH }

    //so, and this is independent of the render backend: each component should get the context object injected into
    //it somehow, in one of these ways:
    // - directly as a constructor parameter (eww)
    // - add a factory method to the context, and it passes itself (yuck)
    // - have a thread local context variable, which the objects automatically grab (kinda nice, but requires
    //   indirection when trying to create objects for multiple windows/contexts in the same scope)
    //
    //the second big choice is whether
    // - graphics operations are basically immediate and we just try to validate that
    //   the thread is correct for a given object's context,
    // - or we have a frontend graphics object (which we will have anyway) which merely enqueues all commands,
    //   and they are then worked off in the update cycle. this option also has the benefit of allowing optimisations
    //   for operation clustering in the backend without additional user effort.
    //constraint: i want to be able to call graphics objects as though in immediate mode even given the indirection
    //of a potential command buffer. most apis apart from opengl already require a command buffer to be configured
    //beforehand anyway, and this would sort of streamline this process.

    open fun bind() {
        bind(Binding.BOTH)
    }

    fun bind(binding: Binding) {
        //TODO maybe move context check here?
        GL30C.glBindFramebuffer(
            when (binding) {
                Binding.READ -> GL30C.GL_READ_FRAMEBUFFER
                Binding.WRITE -> GL30C.GL_DRAW_FRAMEBUFFER
                Binding.BOTH -> GL30C.GL_FRAMEBUFFER
            }, id
        )
        GL20C.glDrawBuffers(glColourDrawBuffers)
        GL11C.glViewport(0, 0, size.x(), size.y())
        context.activeFramebuffer = this
    }

    /**
     * Unbinds the currently bound framebuffer, which is identical to binding the
     * [context's screen buffer](org.etieskrill.engine.window.Window.screenBuffer).
     */
    fun unbind() = context.screenBuffer.bind()

    actual fun clear() {
        bind()
        GL11C.glClearColor(clearColour.x, clearColour.y, clearColour.z, clearColour.w)
        if ((glBufferClearMask and GL11C.GL_DEPTH_BUFFER_BIT) != 0) GL11C.glDepthMask(true)
        if ((glBufferClearMask and GL11C.GL_STENCIL_BUFFER_BIT) != 0) GL11C.glStencilMask(0xFF) //TODO can stencil buffer be anything other than one byte in size?
        GL11C.glClear(glBufferClearMask)
        unbind()
    }

    override fun dispose() {
        GL30C.glDeleteFramebuffers(id)
        attachments.values.forEach(Disposable::dispose)
    }

}
package org.etieskrill.engine.config

import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer

data class GraphicsContext(
    var active: Boolean,
    val maxTextureUnits: Int
) {

    internal var thread: Thread? = null

    lateinit var activeFramebuffer: FrameBuffer internal set

    companion object {
        val CONTEXT: ThreadLocal<GraphicsContext> = ThreadLocal<GraphicsContext>.withInitial {
            error("Tried to access graphics context in non-current thread")
        }

        @get:JvmName("MAX_TEXTURE_UNITS")
        val MAX_TEXTURE_UNITS get() = CONTEXT.get().maxTextureUnits
    }

    internal fun checkThread() =
        check(Thread.currentThread() == thread) {
            "This method may only be called from the thread the graphics context is attached to"
        }

}

package org.etieskrill.engine.graphics

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import kotlin.properties.Delegates.notNull

private val logger = KotlinLogging.logger {}

actual data class GraphicsContext(
    val maxTextureUnits: Int,
) {

    internal var thread: Thread? = null

    actual var screenBuffer: FrameBuffer by notNull(); internal set
    actual var activeFramebuffer: FrameBuffer by notNull(); internal set

    actual fun withContext(block: GraphicsContext.() -> Unit) {
        logger.warn { "Usage of graphics context withContext, but this is not implemented yet" }
        block()
    }

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
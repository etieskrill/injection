package org.etieskrill.engine.graphics

import io.github.etieskrill.injection.extension.shader.Texture
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.shader.Shader
import kotlin.properties.Delegates.notNull

actual data class GraphicsContext(
    val maxTextureUnits: Int,
) {

    internal var thread: Thread? = null

    actual var screenBuffer: FrameBuffer by notNull(); internal set
    var activeFramebuffer: FrameBuffer by notNull(); internal set

    internal val _textureBindings = Array<Texture?>(maxTextureUnits) { null }
    val textureBindings: Array<Texture?> get() = _textureBindings

    internal val _bufferBindings = mutableMapOf<BufferType, BufferObject<*>>()
    val bufferBindings: Map<BufferType, BufferObject<*>> get() = _bufferBindings

    internal val _storageBufferBindings = mutableMapOf<Int, StorageBufferObject<*>>()
    val storageBufferBindings: Map<Int, StorageBufferObject<*>> get() = _storageBufferBindings

    internal var _activeShader: Shader? = null
    val activeShader: Shader? get() = _activeShader

    internal var _activeVertexArray: VertexArrayObject<*>? = null
    val activeVertexArray: VertexArrayObject<*>? get() = _activeVertexArray

    actual fun <T> withContext(block: () -> T): T {
        checkThread() //TODO use rendering coroutine withContext instead
        return block()
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

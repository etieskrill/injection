package org.etieskrill.engine.graphics

import io.github.etieskrill.injection.extension.shader.Texture
import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.BufferObjectInstance
import org.etieskrill.engine.graphics.buffer.BufferType
import org.etieskrill.engine.graphics.buffer.StorageBufferObject
import org.etieskrill.engine.graphics.buffer.StorageBufferObjectInstance
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.buffer.VertexArrayObjectInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.shader.Shader
import kotlin.properties.Delegates.notNull

actual data class GraphicsContext(
    val maxTextureUnits: Int,
    val maxVertexAttributes: Int
) {

    internal var thread: Thread? = null

    actual var screenBuffer: FrameBuffer by notNull(); internal set
    var activeFramebuffer: FrameBuffer by notNull(); internal set

    internal val textureBindings = Array<Texture?>(maxTextureUnits) { null }

    private val bufferObjects = mutableMapOf<BufferObject<*>, BufferObjectInstance<*>>()
    internal fun getBufferObject(buffer: BufferObject<*>) =
        bufferObjects.getOrPut(buffer) { BufferObjectInstance(buffer, this) }

    internal val bufferBindings = mutableMapOf<BufferType, BufferObjectInstance<*>>()

    internal val storageBufferBindings = mutableMapOf<Int, StorageBufferObjectInstance<*>>()

    internal var activeShader: Shader? = null

    private val vertexArrays = mutableMapOf<VertexArrayObject<*>, VertexArrayObjectInstance<*>>()
    internal fun <T> getVertexArray(vertexArray: VertexArrayObject<T>) =
        vertexArrays.getOrPut(vertexArray) { VertexArrayObjectInstance(vertexArray, this) }

    internal var activeVertexArray: VertexArrayObjectInstance<*>? = null

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

        @get:JvmName("MAX_VERTEX_ATTRIBUTES")
        val MAX_VERTEX_ATTRIBUTES get() = CONTEXT.get().maxVertexAttributes
    }

    internal fun checkThread() =
        check(Thread.currentThread() == thread) {
            "This method may only be called from the thread the graphics context is attached to"
        }

}

package org.etieskrill.engine.graphics

import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.BufferObjectInstance
import org.etieskrill.engine.graphics.buffer.BufferType
import org.etieskrill.engine.graphics.buffer.StorageBufferObject
import org.etieskrill.engine.graphics.buffer.StorageBufferObjectInstance
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.buffer.VertexArrayObjectInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBufferInstance
import org.etieskrill.engine.graphics.framebuffer.RenderBuffer
import org.etieskrill.engine.graphics.framebuffer.RenderBufferInstance
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.shader.ShaderInstance
import org.etieskrill.engine.graphics.texture.ArrayTexture2D
import org.etieskrill.engine.graphics.texture.ArrayTexture2DInstance
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.Texture2DInstance
import org.etieskrill.engine.graphics.texture.TextureInstance
import kotlin.properties.Delegates.notNull

//TODO put all withContext-esque wrappers here, including a wrapper for error functions
//we guarantee the construction of an instance will happen while the respective context is active, but any calls outside
//of that will still have to manually use a withContext function
actual data class GraphicsContext(
    val maxTextureUnits: Int,
    val maxVertexAttributes: Int
) {

    internal var thread: Thread? = null

    var activeFramebuffer: FrameBufferInstance by notNull(); internal set
    actual var screenBuffer: FrameBufferInstance by notNull(); internal set
    private val frameBuffers = mutableMapOf<FrameBuffer, FrameBufferInstance>()
    internal fun getFrameBuffer(frameBuffer: FrameBuffer) =
        frameBuffers.getOrPut(frameBuffer) { FrameBufferInstance(frameBuffer, this) }

    private val textures = mutableMapOf<Texture, TextureInstance<*>>()

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Texture> getTexture(texture: T): TextureInstance<T> = textures.getOrPut(texture) {
        when (texture) {
            is Texture2D -> Texture2DInstance(texture, this)
            is ArrayTexture2D -> ArrayTexture2DInstance(texture, this)
            else -> error("Unknown texture type ${texture::class.simpleName}")
        }
    } as TextureInstance<T>

    //TODO other texture types
    internal val textureBindings = Array<TextureInstance<*>?>(maxTextureUnits) { null }

    private val bufferObjects = mutableMapOf<BufferObject<*>, BufferObjectInstance<*>>()
    internal fun getBufferObject(buffer: BufferObject<*>) =
        bufferObjects.getOrPut(buffer) { BufferObjectInstance(buffer, this) }

    internal val bufferBindings = mutableMapOf<BufferType, BufferObjectInstance<*>>()

    private val storageBufferObjects = mutableMapOf<StorageBufferObject<*>, StorageBufferObjectInstance<*>>()
    internal fun getStorageBufferObject(buffer: StorageBufferObject<*>) =
        storageBufferObjects.getOrPut(buffer) { StorageBufferObjectInstance(buffer, this) }

    internal val storageBufferBindings = mutableMapOf<Int, StorageBufferObjectInstance<*>>()

    private val renderBuffers = mutableMapOf<RenderBuffer, RenderBufferInstance>()
    internal fun getRenderBuffer(renderBuffer: RenderBuffer) =
        renderBuffers.getOrPut(renderBuffer) { RenderBufferInstance(renderBuffer, this) }

    private val shaders = mutableMapOf<Shader, ShaderInstance>()
    internal fun getShader(shader: Shader) =
        shaders.getOrPut(shader) { ShaderInstance(shader, this) }

    internal var activeShader: ShaderInstance? = null

    private val vertexArrays = mutableMapOf<VertexArrayObject<*>, VertexArrayObjectInstance<*>>()
    internal fun <T> getVertexArray(vertexArray: VertexArrayObject<T>) =
        vertexArrays.getOrPut(vertexArray) { VertexArrayObjectInstance(vertexArray, this) }

    internal var activeVertexArray: VertexArrayObjectInstance<*>? = null

    internal fun getFrameBufferAttachment(frameBufferAttachment: FrameBufferAttachment): FrameBufferAttachmentInstance =
        when (frameBufferAttachment) {
            is RenderBuffer -> getRenderBuffer(frameBufferAttachment)
            else -> error("Unknown frame buffer attachment type: ${frameBufferAttachment::class.simpleName}")
        }

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

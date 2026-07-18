package org.etieskrill.engine.graphics

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.BufferCreationException
import org.etieskrill.engine.graphics.gl.GLUtils
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER
import java.nio.ByteBuffer

actual open class BufferObject<T>(
    actual val context: GraphicsContext,
    actual override val accessor: BufferAccessor<T>,
    actual val numElements: Int,
    actual val type: BufferType = BufferType.ARRAY,
    actual val accessFrequency: BufferAccessFrequency = BufferAccessFrequency.STATIC,
    actual val accessType: BufferAccessType = BufferAccessType.DRAW
) : Buffer<T>, Disposable {

    actual open val byteSize get() = accessor.elementByteSize * numElements

    init {
        check(type != BufferType.STORAGE) {
            "Generic BufferObject may only be used for ARRAY and ELEMENT_ARRAY types, use StorageBufferObject for STORAGE type instead"
        }

        context.withContext { GLUtils.clearError() }
    }

    protected val id = context.withContext { GL15C.glGenBuffers() }

    actual override val buffer: ByteBuffer by lazy { BufferUtils.createByteBuffer(byteSize) }

    //TODO add vao slot to ensure in-use (bound) buffers are not accidentally used elsewhere

    init {
        context.withContext {
            bind()
            GL15C.glBufferData(type.gl, byteSize.toLong(), toGLUsage(accessFrequency, accessType))

            GLUtils.checkErrorThrowing("Failed to create buffer object of type ${type.name} with size of $byteSize") {
                BufferCreationException(it)
            }
        }
    }

    fun bind() = context.withContext {
        GL15C.glBindBuffer(type.gl, id)
        context._bufferBindings[type] = this
    }

    fun unbind() = context.withContext {
        GL15C.glBindBuffer(type.gl, 0)
        context._bufferBindings.remove(type)
    }

    actual override fun setData(elements: Collection<T>) = accessor.map(elements, this)

    actual fun getData(): ByteBuffer {
        bind()
        GL15C.glGetBufferSubData(type.gl, 0, buffer.clear())
        return buffer //TODO asReadOnlyBuffer?
    }

    /**
     * Sets the buffer's data to the values provided in [data].
     *
     * @param data the data to set in the buffer
     */
    actual override fun setData(data: ByteBuffer) = setData(data, false)

    /**
     * Sets the buffer's data to the values provided in [data]. The buffer is cleared beforehand if the [clear] flag is set.
     *
     * @param data  the data to set in the buffer
     * @param clear if `true`, buffer is cleared to all zeroes before data is set
     */
    actual open fun setData(data: ByteBuffer, clear: Boolean) = setData(0L, data, clear)

    /**
     * Sets the buffer's data at [offset] to the values provided in [data]. The buffer is cleared beforehand if the
     * [clear] flag is set.
     *
     * @param offset offset to the start of the buffer
     * @param data   the data to set in the buffer
     * @param clear  if `true`, buffer is cleared to all zeroes before data is set
     */
    actual open fun setData(offset: Long, data: ByteBuffer, clear: Boolean) = context.withContext {
        //TODO is possible data still set in backend?
        check(data.limit() - data.position() <= byteSize - offset) { "Data buffer size exceeds capacity of BufferObject" }

        data.rewind()
        bind()
        if (clear) {
            GL43C.glClearBufferSubData(
                type.gl,
                GL30C.GL_R8I,
                0.toLong(),
                byteSize.toLong(),
                GL11C.GL_RED,
                GL11C.GL_BYTE,
                null as ByteBuffer?
            )
        }
        GL15C.glBufferSubData(type.gl, offset, data)
    }

    actual override fun dispose() = context.withContext {
        GL15C.glDeleteBuffers(id)
    }

}

internal val BufferType.gl
    get() = when (this) {
        BufferType.ARRAY -> GL_ARRAY_BUFFER
        BufferType.ELEMENT_ARRAY -> GL_ELEMENT_ARRAY_BUFFER
        BufferType.STORAGE -> GL_SHADER_STORAGE_BUFFER
    }

internal fun toGLUsage(frequency: BufferAccessFrequency, accessType: BufferAccessType) = when (frequency) {
    BufferAccessFrequency.STATIC -> when (accessType) {
        BufferAccessType.DRAW -> GL15C.GL_STATIC_DRAW
        BufferAccessType.READ -> GL15C.GL_STATIC_READ
        BufferAccessType.COPY -> GL15C.GL_STATIC_COPY
    }

    BufferAccessFrequency.STREAM -> when (accessType) {
        BufferAccessType.DRAW -> GL15C.GL_STREAM_DRAW
        BufferAccessType.READ -> GL15C.GL_STREAM_READ
        BufferAccessType.COPY -> GL15C.GL_STREAM_COPY
    }

    BufferAccessFrequency.DYNAMIC -> when (accessType) {
        BufferAccessType.DRAW -> GL15C.GL_DYNAMIC_DRAW
        BufferAccessType.READ -> GL15C.GL_DYNAMIC_READ
        BufferAccessType.COPY -> GL15C.GL_DYNAMIC_COPY
    }
}

package org.etieskrill.engine.graphics.buffer

import org.etieskrill.engine.buffer.ByteBuffer
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.gl.BufferCreationException
import org.etieskrill.engine.graphics.gl.GLUtils
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER

internal actual fun BufferObject<*>.checkBufferObject() {
    check(type != BufferType.STORAGE) {
        "Generic BufferObject may only be used for ARRAY and ELEMENT_ARRAY types, use StorageBufferObject for STORAGE type instead"
    }
}

internal actual open class BufferObjectInstance<T>(
    actual open val descriptor: BufferObject<T>,
    actual val context: GraphicsContext,
) : Disposable {

    init {
        GLUtils.clearError()
    }

    protected val id = context.withContext { GL15C.glGenBuffers() }

    internal actual var version = 0L

    //TODO add vao slot to ensure in-use (bound) buffers are not accidentally used elsewhere

    init {
        context.withContext {
            descriptor.apply {
                bind()
                GL15C.glBufferData(type.gl, byteSize.toLong(), toGLUsage(accessFrequency, accessType))

                GLUtils.checkErrorThrowing("Failed to create buffer object of type ${type.name} with size of $byteSize") {
                    BufferCreationException(it)
                }
            }
        }
    }

    fun bind(updateBuffer: Boolean = true) = context.withContext {
        if (context.bufferBindings[descriptor.type] != this) {
            GL15C.glBindBuffer(descriptor.type.gl, id)
            context.bufferBindings[descriptor.type] = this

            if (updateBuffer) syncBuffer()
        }
    }

    fun unbind() = context.withContext {
        if (context.bufferBindings[descriptor.type] == this) {
            GL15C.glBindBuffer(descriptor.type.gl, 0)
            context.bufferBindings.remove(descriptor.type)
        }
    }

    actual fun getData(): ByteBuffer = context.withContext {
        bind(false)
        GL15C.glGetBufferSubData(descriptor.type.gl, 0, descriptor.buffer.buffer.rewind())
        version = descriptor.version++
        descriptor.buffer //TODO asReadOnlyBuffer?
    }

    actual open fun setData(buffer: ByteBuffer): Unit = context.withContext {
        val numElements = buffer.size / descriptor.accessor.elementByteSize
        //TODO move to non-instance
        check(numElements <= descriptor.numElements) {
            "Buffer overflow: tried to insert $numElements into buffer of size ${descriptor.numElements}"
        }
        check(buffer.size.toInt() % descriptor.accessor.elementByteSize == 0) {
            "Buffer contents do not align with element byte boundaries"
        }

        bind(false)
//        GL43C.glClearBufferSubData(
//            type.gl, GL30C.GL_R8I, 0.toLong(), byteSize.toLong(),
//            GL11C.GL_RED, GL11C.GL_BYTE, null as ByteBuffer?
//        )
        GL15C.glBufferSubData(descriptor.type.gl, 0, descriptor.buffer.buffer.flip())
    }

    internal fun syncBuffer() {
        if (version < descriptor.version) {
            setData(descriptor.buffer)
            version = descriptor.version
        }
    }

    actual override fun dispose() = context.withContext {
        unbind()
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

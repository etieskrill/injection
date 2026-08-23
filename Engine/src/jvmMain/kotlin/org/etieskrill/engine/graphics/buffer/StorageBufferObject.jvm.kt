package org.etieskrill.engine.graphics.buffer

import org.etieskrill.engine.buffer.ByteBuffer
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL43C

internal actual class StorageBufferObjectInstance<T>(
    actual override val descriptor: StorageBufferObject<T>,
    context: GraphicsContext
) : BufferObjectInstance<T>(descriptor, context), Disposable {

    val byteSize: Int get() = 4 * Int.SIZE_BYTES + descriptor.numElements * descriptor.accessor.elementByteSize

    actual override fun setData(buffer: ByteBuffer): Unit = context.withContext {
        val numElements = buffer.size / descriptor.accessor.elementByteSize
        check(numElements <= descriptor.numElements) {
            "Buffer overflow: tried to insert $numElements into buffer of size ${descriptor.numElements}"
        }
        check(buffer.size.toInt() % descriptor.accessor.elementByteSize == 0) {
            "Buffer contents do not align with element byte boundaries"
        }

        descriptor.buffer.clear()
        descriptor.buffer += numElements.toInt()
        descriptor.buffer.writeHead += 3 * Int.SIZE_BYTES
        descriptor.buffer.put(buffer)

        bind(false)
        GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0L, descriptor.buffer.buffer.flip())
    }

    //TODO check if bindings other than zero are retrievable from shader object at runtime
    fun bind(binding: Int) = context.withContext {
        if (context.storageBufferBindings[binding] != this) {
            GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, binding, id)
            context.storageBufferBindings[binding] = this

            syncBuffer()
        }
    }

    fun unbind(binding: Int) = context.withContext {
        if (context.storageBufferBindings[binding] != null) {
            GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, binding, 0)
            context.storageBufferBindings.remove(binding)
        }
    }

}

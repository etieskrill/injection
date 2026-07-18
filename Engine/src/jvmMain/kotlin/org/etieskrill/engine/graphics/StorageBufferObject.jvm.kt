package org.etieskrill.engine.graphics

import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import org.etieskrill.engine.common.Disposable
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL43C
import java.nio.ByteBuffer

actual class StorageBufferObject<T> actual constructor(
    context: GraphicsContext,
    numElements: Int,
    accessor: BufferAccessor<T>,
    frequency: BufferAccessFrequency,
    accessType: BufferAccessType
) : BufferObject<T>(context, accessor, numElements, BufferType.STORAGE, frequency, accessType),
    StorageBuffer<T>, Disposable {

    override val buffer: ByteBuffer by lazy { BufferUtils.createByteBuffer(byteSize) } //FIXME necessary?

    actual override val byteSize: Int get() = 4 * Int.SIZE_BYTES + numElements * accessor.elementByteSize

    private val numElementsBuffer = IntArray(4)

    override fun setData(elements: Collection<T>) {
        check(elements.size <= numElements) {
            "Buffer overflow: tried to insert ${elements.size} into buffer of size $numElements"
        }
        accessor.map(elements, this)
    }

    override fun setData(data: ByteBuffer) = context.withContext {
        val numElements = data.limit() / accessor.elementByteSize
        check(numElements <= this.numElements) {
            "Buffer overflow: tried to insert $numElements into buffer of size ${this.numElements}"
        }

        numElementsBuffer[0] = numElements

        bind()
        GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 0L, numElementsBuffer)
        GL15C.glBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, 4L * Int.SIZE_BYTES, data)
    }

    actual override fun setData(data: ByteBuffer, clear: Boolean): Unit = TODO("probably unsupported for ssbo")
    actual override fun setData(offset: Long, data: ByteBuffer, clear: Boolean): Unit =
        TODO("probably unsupported for ssbo")

    //TODO check if bindings other than zero are retrievable from shader object at runtime
    fun bind(binding: Int) = context.withContext {
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, binding, id)
        context._storageBufferBindings[binding] = this
    }

    fun unbind(binding: Int) = context.withContext {
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, binding, 0)
        context._storageBufferBindings.remove(binding)
    }

}

package org.etieskrill.engine.graphics.gl

import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import org.etieskrill.engine.common.Disposable
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER
import java.nio.ByteBuffer

class StorageBufferObject<T>(
    val maxNumElements: Int,
    override val accessor: BufferAccessor<T>,
    frequency: Frequency = Frequency.STREAM,
    accessType: AccessType = AccessType.DRAW
) : BufferObject<T>(accessor, maxNumElements, Target.STORAGE_BUFFER, frequency, accessType),
    StorageBuffer<T>, Disposable {

    override val buffer: ByteBuffer = BufferUtils.createByteBuffer(byteSize)

    override val byteSize get() = 4 * Int.SIZE_BYTES + maxNumElements * accessor.elementByteSize

    private val numElementsBuffer = IntArray(4)

    override fun setData(elements: Collection<T>) {
        check(elements.size <= maxNumElements) {
            "Buffer overflow: tried to insert ${elements.size} into buffer of size $maxNumElements"
        }
        accessor.map(elements, this)
    }

    override fun setData(data: ByteBuffer) {
        val numElements = data.limit() / accessor.elementByteSize
        check(numElements <= maxNumElements) {
            "Buffer overflow: tried to insert $numElements into buffer of size $maxNumElements"
        }

        numElementsBuffer[0] = numElements

        bind()
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, numElementsBuffer)
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 4L * Int.SIZE_BYTES, data)
    }

    override fun setData(data: ByteBuffer, clear: Boolean) = TODO("probably unsupported for ssbo")
    override fun setData(offset: Long, data: ByteBuffer, clear: Boolean) = TODO("probably unsupported for ssbo")

    //TODO check if bindings other than zero are retrievable from shader object at runtime
    override fun bind(binding: Int) = glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, id)
    override fun unbind(binding: Int) = glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, 0)

}

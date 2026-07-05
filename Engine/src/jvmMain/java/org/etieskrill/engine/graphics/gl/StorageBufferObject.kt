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
    numElements: Int,
    accessor: BufferAccessor<T>, //does not override to avoid null in superclass initialisation
    frequency: Frequency = Frequency.STREAM,
    accessType: AccessType = AccessType.DRAW
) : BufferObject<T>(accessor, numElements, Target.STORAGE_BUFFER, frequency, accessType),
    StorageBuffer<T>, Disposable {

    override val buffer: ByteBuffer = BufferUtils.createByteBuffer(byteSize)

    override val byteSize: Int
        get() {
            return 4 * Int.SIZE_BYTES + numElements * accessor.elementByteSize
        }

    private val numElementsBuffer = IntArray(4)

    override fun setData(elements: Collection<T>) {
        check(elements.size <= numElements) {
            "Buffer overflow: tried to insert ${elements.size} into buffer of size $numElements"
        }
        accessor.map(elements, this)
    }

    override fun setData(data: ByteBuffer) {
        val numElements = data.limit() / accessor.elementByteSize
        check(numElements <= this.numElements) {
            "Buffer overflow: tried to insert $numElements into buffer of size ${this.numElements}"
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

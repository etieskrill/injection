package org.etieskrill.engine.graphics.buffer

import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer

class StorageBufferObject<T>(
    numElements: Int,
    accessor: BufferAccessor<T>,
    frequency: BufferAccessFrequency = BufferAccessFrequency.STREAM,
    accessType: BufferAccessType = BufferAccessType.DRAW
) : BufferObject<T>(accessor, numElements, BufferType.STORAGE, frequency, accessType), StorageBuffer<T>

internal expect class StorageBufferObjectInstance<T> : BufferObjectInstance<T> {
    override val descriptor: StorageBufferObject<T>

    override fun setData(buffer: ByteArray)
}

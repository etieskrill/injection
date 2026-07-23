package org.etieskrill.engine.graphics.buffer

import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import java.nio.ByteBuffer

expect class StorageBufferObject<T> : BufferObject<T>, StorageBuffer<T>, Disposable {

    constructor(
        context: GraphicsContext,
        numElements: Int,
        accessor: BufferAccessor<T>,
        frequency: BufferAccessFrequency = BufferAccessFrequency.STREAM,
        accessType: BufferAccessType = BufferAccessType.DRAW
    )

    override val byteSize: Int

    override fun setData(data: ByteBuffer, clear: Boolean)
    override fun setData(offset: Long, data: ByteBuffer, clear: Boolean)

}

package io.github.etieskrill.injection.extension.shader

import java.nio.ByteBuffer

interface BufferAccessor<T> {

    val elementByteSize: Int

    fun map(elements: Collection<T>, buffer: Buffer<T>)

}

interface Buffer<T> {

    /**
     * The CPU-side [ByteBuffer] backing this buffer object. Used for getting and setting data.
     */
    val buffer: ByteBuffer

    val accessor: BufferAccessor<T>

    fun setData(elements: Collection<T>)
    fun setData(data: ByteBuffer)

    fun bind()
    fun unbind()

}

interface StorageBuffer<T> : Buffer<T> {

    fun bind(binding: Int)
    fun unbind(binding: Int)

}

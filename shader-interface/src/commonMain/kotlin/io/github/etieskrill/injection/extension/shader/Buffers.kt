package io.github.etieskrill.injection.extension.shader

interface BufferAccessor<T> {

    val elementByteSize: Int

    fun map(elements: Collection<T>, buffer: Buffer<T>)

}

interface Buffer<T> {

    /**
     * The CPU-side [ByteArray] backing this buffer object. Used for getting and setting data.
     */
    val buffer: ByteArray

    val accessor: BufferAccessor<T>

    //TODO should be property and differentiate type by name
    fun setData(elements: Collection<T>)
    fun setData(data: ByteArray)

}

interface StorageBuffer<T> : Buffer<T>

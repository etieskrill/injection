package org.etieskrill.engine.graphics.buffer

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext

enum class BufferType { ARRAY, ELEMENT_ARRAY, STORAGE }
enum class BufferAccessFrequency { STATIC, STREAM, DYNAMIC }
enum class BufferAccessType { DRAW, READ, COPY }

open class BufferObject<T>(
    override val accessor: BufferAccessor<T>,
    val numElements: Int,

    val type: BufferType = BufferType.ARRAY,
    val accessFrequency: BufferAccessFrequency = BufferAccessFrequency.STATIC,
    val accessType: BufferAccessType = BufferAccessType.DRAW,
) : Buffer<T> {

    init {
        checkBufferObject()
    }

    open val byteSize: Int get() = accessor.elementByteSize * numElements

    /**
     * The CPU-side byte buffer backing this buffer object. Used for getting and setting data.
     * > **Important:** getting this property does **not** read the data from the GPU, use
     * [BufferObjectInstance.getData] for that instead.
     */
    //TODO multiplatform buffer wrappers: ByteBuffer and MutableByteBuffer
    //TODO add flag to retain buffer upon load, and use new buffer for each set by default
    override val buffer: ByteArray = ByteArray(byteSize)

    internal var version = 0L

    override fun setData(elements: Collection<T>) {
        check(elements.size <= numElements) {
            "Buffer overflow: tried to insert ${elements.size} elements into buffer of size $numElements"
        }
        accessor.map(elements, this)
    }

    override fun setData(data: ByteArray) = setData(data, offset = 0)

    /**
     * Sets the buffer's data at [offset] to the values provided in [data]. The buffer is cleared beforehand if the
     * [clear] flag is set.
     *
     * @param offset offset to the start of the buffer
     * @param data   the data to set in the buffer
     * @param clear  if `true`, buffer is cleared to all zeroes before data is set
     */
    fun setData(data: ByteArray, offset: Int = 0) {
        check(data.size <= byteSize - offset) { "Data buffer size exceeds capacity of BufferObject" }

        //TODO is contentEquals check too expensive?
        data.copyInto(buffer, offset)
        version++
    }

}

/**
 * Platform specific checks of buffer configuration.
 */
internal expect fun BufferObject<*>.checkBufferObject()

internal expect open class BufferObjectInstance<T> : Disposable {
    open val descriptor: BufferObject<T>
    val context: GraphicsContext

    internal var version: Long

    /**
     * Returns a byte buffer containing the data in the buffer object. On the first call to this method (or getting
     * [BufferObject.buffer]), a new buffer sized to this buffer object is created, after which point this same object
     * is always returned.
     *
     * Writing to the buffer does not affect the [BufferObject]. Instead, [setData] can be used to write to the object.
     *
     * @return a buffer containing the buffer object's data
     */
    fun getData(): ByteArray
    open fun setData(buffer: ByteArray)

    override fun dispose()
}

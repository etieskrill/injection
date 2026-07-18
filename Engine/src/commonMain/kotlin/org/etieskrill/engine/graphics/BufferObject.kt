package org.etieskrill.engine.graphics

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import org.etieskrill.engine.common.Disposable
import java.nio.ByteBuffer

expect open class BufferObject<T> : Buffer<T>, Disposable {

    val context: GraphicsContext

    override val accessor: BufferAccessor<T>
    val type: BufferType
    val accessFrequency: BufferAccessFrequency
    val accessType: BufferAccessType

    val numElements: Int

    open val byteSize: Int

    /**
     * The CPU-side [ByteBuffer] backing this buffer object. Used for getting and setting data.
     * > **Important:** getting this property does **not** read the data from the GPU, use [getData] for that instead.
     */
    override val buffer: ByteBuffer

    override fun setData(elements: Collection<T>)

    /**
     * Returns a [ByteBuffer] containing the data in the buffer object. On the first call to this method (or getting
     * [buffer], a new buffer sized to this buffer object is created, after which point this same object is always returned.
     *
     * Writing to the buffer does not affect this [BufferObject]. Instead, [setData] can be used to write to the object.
     *
     * @return a buffer containing the buffer object's data
     */
    fun getData(): ByteBuffer

    /**
     * Sets the buffer's data to the values provided in [data].
     *
     * @param data the data to set in the buffer
     */
    override fun setData(data: ByteBuffer)

    /**
     * Sets the buffer's data to the values provided in [data]. The buffer is cleared beforehand if the [clear] flag is set.
     *
     * @param data  the data to set in the buffer
     * @param clear if `true`, buffer is cleared to all zeroes before data is set
     */
    open fun setData(data: ByteBuffer, clear: Boolean)

    /**
     * Sets the buffer's data at [offset] to the values provided in [data]. The buffer is cleared beforehand if the
     * [clear] flag is set.
     *
     * @param offset offset to the start of the buffer
     * @param data   the data to set in the buffer
     * @param clear  if `true`, buffer is cleared to all zeroes before data is set
     */
    open fun setData(offset: Long, data: ByteBuffer, clear: Boolean)

    override fun dispose()

}

enum class BufferType { ARRAY, ELEMENT_ARRAY, STORAGE }
enum class BufferAccessFrequency { STATIC, STREAM, DYNAMIC }
enum class BufferAccessType { DRAW, READ, COPY }

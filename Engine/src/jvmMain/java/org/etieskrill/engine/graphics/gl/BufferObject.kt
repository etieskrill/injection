package org.etieskrill.engine.graphics.gl

import io.github.etieskrill.injection.extension.shader.BufferAccessor
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.BufferObject.AccessType.*
import org.etieskrill.engine.graphics.gl.BufferObject.Frequency.*
import org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing
import org.etieskrill.engine.graphics.gl.GLUtils.clearError
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C.GL_BYTE
import org.lwjgl.opengl.GL11C.GL_RED
import org.lwjgl.opengl.GL15C.*
import org.lwjgl.opengl.GL30C.GL_R8I
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.opengl.GL43C.glClearBufferSubData
import java.nio.ByteBuffer
import io.github.etieskrill.injection.extension.shader.Buffer as StdBuffer

open class BufferObject<T>(
    override val accessor: BufferAccessor<T>,
    val numElements: Int,
    val target: Target = Target.ARRAY,
    frequency: Frequency? = STATIC,
    accessType: AccessType? = DRAW
) : StdBuffer<T>, Disposable {

    open val byteSize get() = accessor.elementByteSize * numElements

    init {
        clearError()
    }

    protected val id = glGenBuffers() //TODO context ensurance stuff, WebGPU may actually be better here

    override val buffer: ByteBuffer by lazy {
        bind()
        BufferUtils.createByteBuffer(byteSize)
        //TODO should actually get data too, maybe based on flag set?
    }

    //TODO add vao slot to ensure in-use (bound) buffers are not accidentally used elsewhere

    init {
        bind()
        glBufferData(target.gl, byteSize.toLong(), toGLUsage(frequency ?: STATIC, accessType ?: DRAW))

        checkErrorThrowing("Failed to create buffer object of type ${target.name} with size of $byteSize") {
            BufferCreationException(it)
        }
    }

    enum class Target(val gl: Int) {
        ARRAY(GL_ARRAY_BUFFER), ELEMENT_ARRAY(GL_ELEMENT_ARRAY_BUFFER), STORAGE_BUFFER(GL_SHADER_STORAGE_BUFFER)
    }

    enum class Frequency { STATIC, STREAM, DYNAMIC }
    enum class AccessType { DRAW, READ, COPY }

    companion object {
        private fun toGLUsage(frequency: Frequency, accessType: AccessType) = when (frequency) {
            STATIC -> when (accessType) {
                DRAW -> GL_STATIC_DRAW
                READ -> GL_STATIC_READ
                COPY -> GL_STATIC_COPY
            }

            STREAM -> when (accessType) {
                DRAW -> GL_STREAM_DRAW
                READ -> GL_STREAM_READ
                COPY -> GL_STREAM_COPY
            }

            DYNAMIC -> when (accessType) {
                DRAW -> GL_DYNAMIC_DRAW
                READ -> GL_DYNAMIC_READ
                COPY -> GL_DYNAMIC_COPY
            }
        }
    }

    override fun bind() = glBindBuffer(target.gl, id)
    override fun unbind() = glBindBuffer(target.gl, 0)

    /**
     * Returns a [ByteBuffer] containing the data in the buffer object. On the first call to this method (or getting
     * [buffer], a new buffer sized to this buffer object is created, after which point this same object is always returned.
     *
     * Writing to the buffer does not affect this [BufferObject]. Instead, [setData] can be used to write to the object.
     *
     * @return a buffer containing the buffer object's data
     */
    fun getData(): ByteBuffer {
        bind()
        glGetBufferSubData(target.gl, 0, buffer.clear())
        return buffer //TODO asReadOnlyBuffer?
    }

    override fun setData(elements: Collection<T>) = accessor.map(elements, this)

    /**
     * Sets the buffer's data to the values provided in [data].
     *
     * @param data the data to set in the buffer
     */
    override fun setData(data: ByteBuffer) = setData(data, false)

    /**
     * Sets the buffer's data to the values provided in [data]. The buffer is cleared beforehand if the [clear] flag is set.
     *
     * @param data  the data to set in the buffer
     * @param clear if `true`, buffer is cleared to all zeroes before data is set
     */
    open fun setData(data: ByteBuffer, clear: Boolean) = setData(0L, data, clear)

    /**
     * Sets the buffer's data at [offset] to the values provided in [data]. The buffer is cleared beforehand if the
     * [clear] flag is set.
     *
     * @param offset offset to the start of the buffer
     * @param data   the data to set in the buffer
     * @param clear  if `true`, buffer is cleared to all zeroes before data is set
     */
    open fun setData(offset: Long, data: ByteBuffer, clear: Boolean) {
        //TODO is possible data still set in backend?
        check(data.limit() - data.position() <= byteSize - offset) { "Data buffer size exceeds capacity of BufferObject" }

        data.rewind()
        bind()
        if (clear) {
            glClearBufferSubData(target.gl, GL_R8I, 0.toLong(), byteSize.toLong(), GL_RED, GL_BYTE, null as ByteBuffer?)
        }
        glBufferSubData(target.gl, offset, data)
    }

    override fun dispose() {
        glDeleteBuffers(id)
    }

}

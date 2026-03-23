package org.etieskrill.engine.graphics.gl

import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import org.etieskrill.engine.Disposable
import org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glBufferData
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL15C.glDeleteBuffers
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.opengl.GL45C.glCreateBuffers
import java.nio.ByteBuffer

//TODO actually a specialisation of the BufferObject, but rewrite *that* first
class StorageBufferObject<T> : StorageBuffer<T>, Disposable {

    val maxNumElements: Int
    val byteSize: Long

    override val buffer: ByteBuffer

    override val accessor: BufferAccessor<T>

    private val id: Int = glCreateBuffers() //TODO context ensurance stuff, WebGPU may actually be better here

    constructor(maxNumElements: Int, accessor: BufferAccessor<T>) {
        this.maxNumElements = maxNumElements
        this.byteSize = maxNumElements * accessor.elementByteSize.toLong()
        this.buffer = BufferUtils.createByteBuffer(byteSize.toInt())
        this.accessor = accessor
        init()
    }

    constructor(data: Collection<T>, accessor: BufferAccessor<T>) {
        this.maxNumElements = data.size
        this.byteSize = data.size * accessor.elementByteSize.toLong()
        this.buffer = BufferUtils.createByteBuffer(byteSize.toInt())
        this.accessor = accessor
        init()
        setData(data)
    }

    private fun init() { //putting this in an actual init {} block does not work bcs var init order is not honoured???
        bind()
        glBufferData(GL_SHADER_STORAGE_BUFFER, byteSize.toLong(), GL15C.GL_STREAM_DRAW) //TODO usage bits

        checkErrorThrowing( //TODO let this be a monument to both testing and eager error checking; for they vanquished the dragons that once were
            "Failed to create storage buffer object with size of $byteSize bytes"
        ) { BufferCreationException(it) }
    }

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

        bind()
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, data)
    }

    override fun bind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, id)
    }

    override fun unbind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
    }

    override fun bind(binding: Int) { //TODO check if bindings other than zero are retrievable from shader object at runtime
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, id)
    }

    override fun unbind(binding: Int) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, 0)
    }

    override fun dispose() {
        glDeleteBuffers(id)
    }

}

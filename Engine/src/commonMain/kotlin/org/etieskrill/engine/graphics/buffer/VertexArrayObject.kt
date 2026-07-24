package org.etieskrill.engine.graphics.buffer

import io.github.etieskrill.injection.extension.shader.Buffer
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext

data class VertexArrayObject<T>(
    val accessor: VertexArrayAccessor<T>,
    val vertexBuffer: BufferObject<T>,
    val indexBuffer: BufferObject<Int>?
) {
    init {
        require(vertexBuffer.type == BufferType.ARRAY) { "Vertex buffer must have ARRAY buffer type" }
        require(indexBuffer == null || indexBuffer.type == BufferType.ELEMENT_ARRAY) { "Index buffer must have ELEMENT_ARRAY buffer type" }
    }

    val isIndexed get() = indexBuffer != null
    val numElements get() = (indexBuffer ?: vertexBuffer).numElements
    val elementByteSize get() = accessor.elementByteSize

    var vertices: Collection<T> get() = TODO(); set(value) = vertexBuffer.setData(value)
    var indices: Collection<Int> get() = TODO(); set(value) {
        check(isIndexed) { "Vertex array object is not indexed" }
        indexBuffer?.setData(value)
    }

    object IndexArrayAccessor : VertexArrayAccessor<Int>() {
        override val elementByteSize: Int get() = super.elementByteSize
        override fun registerFields() = addField<Int> { index, buffer -> buffer.putInt(index) }
    }
}

internal expect class VertexArrayObjectInstance<T> : Disposable {
    val descriptor: VertexArrayObject<T>
    val context: GraphicsContext

    override fun dispose()
}

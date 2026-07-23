package org.etieskrill.engine.graphics.buffer

import io.github.etieskrill.injection.extension.shader.Buffer
import org.etieskrill.engine.common.Disposable

expect class VertexArrayObject<T> : Disposable {

    var vertices: Collection<T>
    var indices: Collection<Int>

    val accessor: VertexArrayAccessor<T>
    val vertexBuffer: BufferObject<T>
    val indexBuffer: BufferObject<Int>?

    val isIndexed: Boolean
    val numElements: Int
    val elementByteSize: Int

    //FIXME needed?
    fun map(vertices: Collection<T>, buffer: Buffer<T>)

    constructor(
        accessor: VertexArrayAccessor<T>, vertexBuffer: BufferObject<T>,
        indexBuffer: BufferObject<Int>? = null, indices: Collection<Int>? = null, numIndices: Int? = null,
        frequency: BufferAccessFrequency? = null, accessType: BufferAccessType? = null
    )

    constructor(
        accessor: VertexArrayAccessor<T>, vertexElements: Collection<T>,
        indexBuffer: BufferObject<Int>? = null, indices: Collection<Int>? = null, numIndices: Int? = null,
        frequency: BufferAccessFrequency? = null, accessType: BufferAccessType? = null
    )

    constructor(
        accessor: VertexArrayAccessor<T>, numVertexElements: Int,
        indexBuffer: BufferObject<Int>? = null, indices: Collection<Int>? = null, numIndices: Int? = null,
        frequency: BufferAccessFrequency? = null, accessType: BufferAccessType? = null
    )

}

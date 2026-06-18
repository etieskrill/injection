package org.etieskrill.engine.graphics.gl

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.BufferObject.Target.ELEMENT_ARRAY
import org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing
import org.lwjgl.opengl.GL20C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20C.glVertexAttribPointer
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glDeleteVertexArrays
import org.lwjgl.opengl.GL30C.glGenVertexArrays
import org.lwjgl.opengl.GL30C.glVertexAttribIPointer
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

/**
 * @param T type of vertex data
 */
@ConsistentCopyVisibility
data class VertexArrayObject<T> private constructor(
    val accessor: VertexArrayAccessor<T>,
    val vertexBuffer: BufferObject<T>,
    val indexBuffer: BufferObject<Int>?
) : Disposable {

    val isIndexed get() = indexBuffer != null
    val numElements get() = (indexBuffer ?: vertexBuffer).numElements
    val elementByteSize get() = vertexBuffer.byteSize

    var vertices: Collection<T> get() = TODO(); set(value) = vertexBuffer.setData(value)
    var indices: Collection<Int>
        get() = TODO()
        set(value) {
            require(isIndexed) { "Vertex array object is not indexed" }
            indexBuffer!!.setData(value)
        }

    private val id = glGenVertexArrays()

    init {
        bind()
    }

    constructor(
        accessor: VertexArrayAccessor<T>, vertexBuffer: BufferObject<T>,
        indexBuffer: BufferObject<Int>? = null, indices: Collection<Int>? = null, numIndices: Int? = null,
        frequency: BufferObject.Frequency? = null, accessType: BufferObject.AccessType? = null
    ) : this(accessor, vertexBuffer, createIndexBuffer(indexBuffer, indices, numIndices, frequency, accessType))

    constructor(
        accessor: VertexArrayAccessor<T>, vertexElements: Collection<T>,
        indexBuffer: BufferObject<Int>? = null, indices: Collection<Int>? = null, numIndices: Int? = null,
        frequency: BufferObject.Frequency? = null, accessType: BufferObject.AccessType? = null
    ) : this(
        accessor,
        BufferObject(accessor, vertexElements.size, frequency = frequency, accessType = accessType)
            .also { it.bind() }
            .also { it.setData(vertexElements) },
        createIndexBuffer(indexBuffer, indices, numIndices, frequency, accessType)
    )

    constructor(
        accessor: VertexArrayAccessor<T>, numVertexElements: Int,
        indexBuffer: BufferObject<Int>? = null, indices: Collection<Int>? = null, numIndices: Int? = null,
        frequency: BufferObject.Frequency? = null, accessType: BufferObject.AccessType? = null
    ) : this(
        accessor,
        BufferObject(accessor, numVertexElements, frequency = frequency, accessType = accessType)
            .also { it.bind() },
        createIndexBuffer(indexBuffer, indices, numIndices, frequency, accessType)
    )

    init {
        require(vertexBuffer.target == BufferObject.Target.ARRAY) { "Vertex buffer must an array buffer" }
        require(indexBuffer?.target?.equals(ELEMENT_ARRAY) ?: true) {
            "Index buffer must an element array buffer"
        }

        configureAttributeArrays(accessor)

        checkErrorThrowing("Failed to create vertex array object") { BufferCreationException(it) }
    }

    companion object {
        const val MAX_VERTEX_ATTRIB_BINDINGS = 16

        fun createIndexBuffer(
            indexBuffer: BufferObject<Int>?,
            indices: Collection<Int>?,
            numIndices: Int?,
            frequency: BufferObject.Frequency?,
            accessType: BufferObject.AccessType?
        ): BufferObject<Int>? = when {
            indexBuffer != null -> indexBuffer
            indices != null -> BufferObject(IndexArrayAccessor, indices.size, ELEMENT_ARRAY, frequency, accessType)
                .also { it.bind() }
                .also { it.setData(indices) }

            numIndices != null -> BufferObject(IndexArrayAccessor, numIndices, ELEMENT_ARRAY, frequency, accessType)
                .also { it.bind() }

            else -> null
        }
    }

    private fun configureAttributeArrays(accessor: VertexArrayAccessor<T>) {
        val totalStrideBytes = accessor.elementByteSize

        var bindingIndex = 0
        var currentStrideBytes = 0L

        val configLog = buildString {
            accessor.fields.forEach { field ->
                for (matrixRow in 0 until field.numMatrixRows) {
                    glEnableVertexAttribArray(bindingIndex)
                    when (field.componentType) {
                        Int::class.java, Byte::class.java, Short::class.java ->
                            glVertexAttribIPointer(
                                bindingIndex,
                                field.numComponents, field.glComponentType,
                                totalStrideBytes, currentStrideBytes
                            )

                        else -> //TODO attrib divisors for instancing
                            glVertexAttribPointer(
                                bindingIndex,
                                field.numComponents, field.glComponentType,
                                field.isNormalised,
                                totalStrideBytes, currentStrideBytes
                            )
                    }

                    append("\tbinding index $bindingIndex to ${field.type.simpleName}")
                    append(" (transformed to ${field.numComponents}d-${field.componentType.simpleName!!.lowercase()}")
                    if (field.numMatrixRows > 1) append(", matrix row ${matrixRow + 1} of ${field.numMatrixRows}")
                    append(")")
                    if (field.isNormalised) append(" normalised")
                    append(" with offset of $currentStrideBytes bytes, and total stride of $totalStrideBytes bytes\n")

                    bindingIndex++
                    currentStrideBytes += field.numComponents * field.componentByteSize
                }
            }

            if (bindingIndex >= MAX_VERTEX_ATTRIB_BINDINGS) {
                throw BufferCreationException("Too many vertex attribute bindings at field $bindingIndex")
            }

            removeSuffix("\n")
        }

        logger.debug { "Configured vertex attribute pointers:\n$configLog" }
    }

    private object IndexArrayAccessor : VertexArrayAccessor<Int>() {
        override val elementByteSize: Int get() = super.elementByteSize
        override fun registerFields() = addField<Int> { index, buffer -> buffer.putInt(index) }
    }

    fun map(vertices: Collection<T>, buffer: Buffer<T>) = accessor.map(vertices, buffer)

    fun bind() = glBindVertexArray(id)
    fun unbind() = glBindVertexArray(0)

    override fun dispose() {
        glDeleteVertexArrays(id)
        vertexBuffer.dispose()
        indexBuffer?.dispose()
    }

}

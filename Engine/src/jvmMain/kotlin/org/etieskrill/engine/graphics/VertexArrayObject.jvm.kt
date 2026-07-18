package org.etieskrill.engine.graphics

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.BufferCreationException
import org.etieskrill.engine.graphics.BufferType.ELEMENT_ARRAY
import org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing
import org.etieskrill.engine.graphics.gl.GLUtils.clearError
import org.lwjgl.opengl.GL20C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20C.glVertexAttribPointer
import org.lwjgl.opengl.GL30C.*

private val logger = KotlinLogging.logger {}

/**
 * @param T type of vertex data
 */
@ConsistentCopyVisibility
actual data class VertexArrayObject<T> private constructor(
    actual val accessor: VertexArrayAccessor<T>,
    actual val vertexBuffer: BufferObject<T>,
    actual val indexBuffer: BufferObject<Int>?
) : Disposable {

    actual val isIndexed get() = indexBuffer != null
    actual val numElements get() = (indexBuffer ?: vertexBuffer).numElements
    actual val elementByteSize get() = accessor.elementByteSize

    actual var vertices: Collection<T> get() = TODO(); set(value) = vertexBuffer.setData(value)
    actual var indices: Collection<Int>
        get() = TODO()
        set(value) {
            require(isIndexed) { "Vertex array object is not indexed" }
            indexBuffer!!.setData(value)
        }

    private val id = glGenVertexArrays()

    actual constructor(
        accessor: VertexArrayAccessor<T>, vertexBuffer: BufferObject<T>,
        indexBuffer: BufferObject<Int>?, indices: Collection<Int>?, numIndices: Int?,
        frequency: BufferAccessFrequency?, accessType: BufferAccessType?
    ) : this(accessor, vertexBuffer, createIndexBuffer(indexBuffer, indices, numIndices, frequency, accessType))

    actual constructor(
        accessor: VertexArrayAccessor<T>, vertexElements: Collection<T>,
        indexBuffer: BufferObject<Int>?, indices: Collection<Int>?, numIndices: Int?,
        frequency: BufferAccessFrequency?, accessType: BufferAccessType?
    ) : this(
        accessor,
        createVertexBuffer(accessor, vertexElements.size, frequency, accessType)
            .also { it.bind() }
            .also { it.setData(vertexElements) },
        createIndexBuffer(indexBuffer, indices, numIndices, frequency, accessType)
    )

    actual constructor(
        accessor: VertexArrayAccessor<T>, numVertexElements: Int,
        indexBuffer: BufferObject<Int>?, indices: Collection<Int>?, numIndices: Int?,
        frequency: BufferAccessFrequency?, accessType: BufferAccessType?
    ) : this(
        accessor,
        createVertexBuffer(accessor, numVertexElements, frequency, accessType)
            .also { it.bind() },
        createIndexBuffer(indexBuffer, indices, numIndices, frequency, accessType)
    )

    init {
        require(vertexBuffer.type == BufferType.ARRAY) { "Vertex buffer must an array buffer" }
        require(indexBuffer == null || indexBuffer.type == ELEMENT_ARRAY) {
            "Index buffer must an element array buffer"
        }

        clearError()

        bind()

        vertexBuffer.bind()
        indexBuffer?.bind()

        configureAttributeArrays(accessor)

        checkErrorThrowing("Failed to create vertex array object") { BufferCreationException(it) }
    }

    companion object {
        const val MAX_VERTEX_ATTRIB_BINDINGS = 16

        private fun <T> createVertexBuffer(
            accessor: VertexArrayAccessor<T>,
            numElements: Int,
            accessFrequency: BufferAccessFrequency?,
            accessType: BufferAccessType?
        ): BufferObject<T> = BufferObject(
            accessor, numElements, BufferType.ARRAY,
            accessFrequency ?: BufferAccessFrequency.STATIC,
            accessType ?: BufferAccessType.DRAW
        )

        fun createIndexBuffer(
            indexBuffer: BufferObject<Int>?,
            indices: Collection<Int>?,
            numIndices: Int?,
            frequency: BufferAccessFrequency?,
            accessType: BufferAccessType?
        ): BufferObject<Int>? = when {
            indexBuffer != null -> indexBuffer
            indices != null -> BufferObject(
                IndexArrayAccessor, indices.size, ELEMENT_ARRAY,
                frequency ?: BufferAccessFrequency.STATIC, accessType ?: BufferAccessType.DRAW
            )
                .also { it.bind() }
                .also { it.setData(indices) }

            numIndices != null -> BufferObject(
                IndexArrayAccessor, numIndices, ELEMENT_ARRAY,
                frequency ?: BufferAccessFrequency.STATIC, accessType ?: BufferAccessType.DRAW
            )
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
                        Int::class, Byte::class, Short::class ->
                            glVertexAttribIPointer(
                                bindingIndex,
                                field.numComponents, field.glComponentType,
                                totalStrideBytes, currentStrideBytes
                            )

                        Float::class, Double::class -> //TODO attrib divisors for instancing
                            glVertexAttribPointer(
                                bindingIndex,
                                field.numComponents, field.glComponentType,
                                field.isNormalised,
                                totalStrideBytes, currentStrideBytes
                            )

                        else -> error("Unsupported vertex array component type: ${field.componentType.simpleName}")
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

    actual fun map(vertices: Collection<T>, buffer: Buffer<T>) = accessor.map(vertices, buffer)

    fun bind() = glBindVertexArray(id)
    fun unbind() = glBindVertexArray(0)

    override fun dispose() {
        glDeleteVertexArrays(id)
        vertexBuffer.dispose()
        indexBuffer?.dispose()
    }

    private val VertexArrayAccessor<*>.FieldAccessor<*>.glComponentType
        get() = when (this.componentType) {
            Int::class -> GL_INT
            Float::class -> GL_FLOAT
            Double::class -> GL_DOUBLE
            Byte::class -> GL_BYTE
            Short::class -> GL_SHORT
            else -> error("Cannot determine gl component type for type: $type")
        }

}

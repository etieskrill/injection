package org.etieskrill.engine.graphics.buffer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.gl.BufferCreationException
import org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing
import org.etieskrill.engine.graphics.gl.GLUtils.clearError
import org.lwjgl.opengl.GL20C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20C.glVertexAttribPointer
import org.lwjgl.opengl.GL30C.*

private val logger = KotlinLogging.logger {}

/**
 * @param T type of vertex data
 */
internal actual data class VertexArrayObjectInstance<T>(
    actual val descriptor: VertexArrayObject<T>,
    actual val context: GraphicsContext
) : Disposable {

    private val id = glGenVertexArrays()

    private val vertexBuffer get() = context.getBufferObject(descriptor.vertexBuffer)
    private val indexBuffer get() = descriptor.indexBuffer?.let { context.getBufferObject(it) }

    init {
        context.withContext {
            clearError()

            bind()
            vertexBuffer.bind()
            indexBuffer?.bind()

            configureAttributeArrays(descriptor.accessor)

            checkErrorThrowing("Failed to create vertex array object") { BufferCreationException(it) }
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

            if (bindingIndex >= context.maxVertexAttributes) {
                throw BufferCreationException("Too many vertex attribute bindings at field $bindingIndex")
            }

            removeSuffix("\n")
        }

        logger.debug { "Configured vertex attribute pointers:\n$configLog" }
    }

    fun bind() = context.withContext {
        if (context.activeVertexArray != this) {
            vertexBuffer.syncBuffer()
            indexBuffer?.syncBuffer()

            glBindVertexArray(id)
            context.activeVertexArray = this
        }
    }

    fun unbind() = context.withContext {
        if (context.activeVertexArray != null) {
            glBindVertexArray(0)
            context.activeVertexArray = null
        }
    }

    actual override fun dispose(): Unit = context.withContext {
        unbind()
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

package org.etieskrill.engine.graphics.gl

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import org.joml.Matrix2d
import org.joml.Matrix2dc
import org.joml.Matrix2f
import org.joml.Matrix2fc
import org.joml.Matrix3d
import org.joml.Matrix3dc
import org.joml.Matrix3f
import org.joml.Matrix3fc
import org.joml.Matrix3x2d
import org.joml.Matrix3x2dc
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Matrix4x3d
import org.joml.Matrix4x3dc
import org.joml.Matrix4x3f
import org.joml.Matrix4x3fc
import org.joml.Vector2d
import org.joml.Vector2dc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.Vector4d
import org.joml.Vector4dc
import org.joml.Vector4f
import org.joml.Vector4fc
import org.joml.Vector4i
import org.joml.Vector4ic
import org.lwjgl.opengl.GL11C.*
import java.nio.ByteBuffer
import kotlin.reflect.KClass

typealias FieldMapper<T> = (T, ByteBuffer) -> Unit

/**
 * @param T type of data accessed
 */
abstract class VertexArrayAccessor<T> : BufferAccessor<T> {

    internal val fields: MutableList<FieldAccessor<*>> = mutableListOf()

    init {
        registerFields()
    }

    override val elementByteSize: Int = fields.sumOf { it.fieldByteSize }

    override fun map(elements: Collection<T>, buffer: Buffer<T>) {
        val byteBuffer = buffer.buffer
            .rewind()
            .limit(elementByteSize * elements.size)

        var position = 0
        for (element in elements) {
            for (field in fields) {
                byteBuffer.position(position)
                position += field.fieldByteSize
                field.accessor(element, byteBuffer)
            }
        }

        check(position == byteBuffer.limit()) { "Vertex buffer position does not align with data byte length" }

        buffer.setData(byteBuffer.flip())
    }

    internal inner class FieldAccessor<F : Any>(
        val type: KClass<F>,
        componentType: KClass<out Number>? = null,
        val accessor: FieldMapper<T>,
        val isNormalised: Boolean
    ) {
        val componentType: KClass<out Number> = componentType ?: parseComponentType(type)

        val numComponents: Int = parseComponentNumber(type)
        val numMatrixRows: Int = parseMatrixRowNumber(type)
        val glComponentType: Int = toGlComponentType(this.componentType)
        val componentByteSize: Int = getComponentByteSize(this.componentType)
        val fieldByteSize: Int = numComponents * numMatrixRows * componentByteSize
    }

    protected inline fun <reified F> addField(
        componentType: KClass<out Number>? = null,
        normalised: Boolean = false,
        noinline mapper: FieldMapper<T>
    ) = addField(F::class, componentType, normalised, mapper)

    protected fun addField(
        type: KClass<*>,
        componentType: KClass<out Number>? = null,
        normalised: Boolean = false,
        mapper: FieldMapper<T>
    ) {
        fields += FieldAccessor(type, componentType, mapper, normalised)
    }

    protected abstract fun registerFields()

    private companion object {
        private fun parseComponentType(type: KClass<*>): KClass<out Number> = when (type) {
            in listOf(
                Int::class, Boolean::class, Vector2i::class, Vector2ic::class, Vector3i::class, Vector3ic::class,
                Vector4i::class, Vector4ic::class
            ) -> Int::class

            in listOf(
                Float::class, Vector2f::class, Vector2fc::class, Vector3f::class, Vector3fc::class,
                Vector4f::class, Vector4fc::class
            ) -> Float::class

            in listOf(
                Matrix2f::class, Matrix2fc::class, Matrix3f::class, Matrix3fc::class, Matrix3x2f::class,
                Matrix3x2fc::class, Matrix4f::class, Matrix4fc::class, Matrix4x3f::class, Matrix4x3fc::class
            )
                -> Float::class

            in listOf(
                Double::class, Vector2d::class, Vector2dc::class, Vector3d::class, Vector3dc::class,
                Vector4d::class, Vector4dc::class
            ) -> Double::class

            in listOf(
                Matrix2d::class, Matrix2dc::class, Matrix3d::class, Matrix3dc::class, Matrix3x2d::class,
                Matrix3x2dc::class, Matrix4d::class, Matrix4dc::class, Matrix4x3d::class, Matrix4x3dc::class
            )
                -> Double::class

            in listOf(Byte::class) -> Byte::class
            in listOf(Short::class) -> Short::class
            else -> error("Cannot determine component type for type: $type")
        }

        private fun parseComponentNumber(type: KClass<*>): Int = when (type) {
            in listOf(Int::class, Long::class, Boolean::class, Float::class, Double::class) -> 1
            in listOf(
                Vector2i::class, Vector2f::class, Vector2d::class, Matrix2f::class, Matrix2d::class,
                Vector2ic::class, Vector2fc::class, Vector2dc::class, Matrix2fc::class, Matrix2dc::class
            ) -> 2

            in listOf(
                Vector3i::class, Vector3f::class, Vector3d::class, Vector3ic::class, Vector3fc::class, Vector3dc::class
            ) -> 3

            in listOf(
                Matrix3f::class, Matrix3d::class, Matrix3x2f::class, Matrix3x2d::class,
                Matrix3fc::class, Matrix3dc::class, Matrix3x2fc::class, Matrix3x2dc::class
            ) -> 3

            in listOf(
                Vector4i::class, Vector4f::class, Vector4d::class, Vector4ic::class, Vector4fc::class, Vector4dc::class
            ) -> 4

            in listOf(
                Matrix4f::class, Matrix4d::class, Matrix4x3f::class, Matrix4x3d::class,
                Matrix4fc::class, Matrix4dc::class, Matrix4x3fc::class, Matrix4x3dc::class
            ) -> 4

            else -> error("Cannot determine component number for type: $type")
        }

        private fun parseMatrixRowNumber(type: KClass<*>): Int = when (type) {
            in listOf(
                Matrix2f::class, Matrix2d::class, Matrix3x2f::class, Matrix3x2d::class,
                Matrix2fc::class, Matrix2dc::class, Matrix3x2fc::class, Matrix3x2dc::class
            ) -> 2

            in listOf(
                Matrix3f::class, Matrix3d::class, Matrix4x3f::class, Matrix4x3d::class,
                Matrix3fc::class, Matrix3dc::class, Matrix4x3fc::class, Matrix4x3dc::class
            ) -> 3

            in listOf(Matrix4f::class, Matrix4d::class, Matrix4fc::class, Matrix4dc::class) -> 4
            else -> 1 //Unexpected types throw above already, so no further types needed
        }

        private fun toGlComponentType(type: KClass<out Number>): Int = when (type) {
            Int::class -> GL_INT
            Float::class -> GL_FLOAT
            Double::class -> GL_DOUBLE
            Byte::class -> GL_BYTE
            Short::class -> GL_SHORT
            else -> error("Cannot determine gl component type for type: $type")
        }

        private fun getComponentByteSize(type: KClass<out Number>): Int = when (type) {
            Int::class -> Int.SIZE_BYTES
            Float::class -> Float.SIZE_BYTES
            Double::class -> Double.SIZE_BYTES
            Byte::class -> Byte.SIZE_BYTES
            Short::class -> Short.SIZE_BYTES
            else -> error("Cannot determine byte size for component type: $type")
        }
    }

}

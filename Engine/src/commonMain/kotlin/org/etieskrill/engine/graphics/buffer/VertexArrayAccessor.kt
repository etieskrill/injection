package org.etieskrill.engine.graphics.buffer

import io.github.etieskrill.injection.extension.shader.Buffer
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import net.bifreus.kml.Mat2
import net.bifreus.kml.Mat2c
import net.bifreus.kml.Mat3
import net.bifreus.kml.Mat3c
import net.bifreus.kml.Mat4
import net.bifreus.kml.Mat4c
import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec3i
import net.bifreus.kml.Vec3ic
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import net.bifreus.kml.Vec4i
import net.bifreus.kml.Vec4ic
import org.etieskrill.engine.buffer.ByteBuffer
import kotlin.reflect.KClass

//TODO replace ByteBuffer with... wrapper? is there an equivalent for native?
typealias FieldMapper<T> = (T, ByteBuffer) -> Unit

/**
 * @param T type of data accessed
 */
abstract class VertexArrayAccessor<T>() : BufferAccessor<T> {

    internal val fields: MutableList<FieldAccessor<*>> = mutableListOf()

    init {
        registerFields()
    }

    override val elementByteSize: Int = fields.sumOf { it.fieldByteSize }

    override fun map(elements: Collection<T>, buffer: Buffer<T>) {
        val byteBuffer = (buffer.buffer as ByteBuffer)
        byteBuffer.clear()

        var position = 0
        for (element in elements) {
            for (field in fields) {
                byteBuffer.writeHead = position.toLong()
                position += field.fieldByteSize
                field.accessor(element, byteBuffer)
            }
        }

        check(position == elementByteSize * elements.size) { "Vertex buffer position does not align with data byte length" }
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

    // @formatter:off
    private companion object {
        private fun parseComponentType(type: KClass<*>): KClass<out Number> = when (type) {
            in listOf(Int::class, Boolean::class, Vec2i::class, Vec2ic::class,
                      Vec3i::class, Vec3ic::class, Vec4i::class, Vec4ic::class
            ) -> Int::class
            in listOf(Float::class, Vec2::class, Vec2c::class, Vec3::class, Vec3c::class, Vec4::class, Vec4c::class,
                      Mat2::class, Mat2c::class, Mat3::class, Mat3c::class, Mat4::class, Mat4c::class
            ) -> Float::class
            else -> error("Cannot determine component type for type: $type")
        }

        private fun parseComponentNumber(type: KClass<*>): Int = when (type) {
            in listOf(Int::class, Boolean::class, Float::class) -> 1
            in listOf(Vec2i::class, Vec2::class, Mat2::class, Vec2ic::class, Vec2c::class, Mat2c::class) -> 2
            in listOf(Vec3i::class, Vec3::class, Vec3ic::class, Vec3c::class, Mat3::class, Mat3c::class) -> 3
            in listOf(Vec4i::class, Vec4::class, Vec4ic::class, Vec4c::class, Mat4::class, Mat4c::class) -> 4
            else -> error("Cannot determine component number for type: $type")
        }

        private fun parseMatrixRowNumber(type: KClass<*>): Int = when (type) {
            in listOf(Mat2::class, Mat2c::class) -> 2
            in listOf(Mat3::class, Mat3c::class) -> 3
            in listOf(Mat4::class, Mat4c::class) -> 4
            else -> 1 //Unexpected types throw above already, so no further types needed
        }

        private fun getComponentByteSize(type: KClass<out Number>): Int = when (type) {
            Int::class -> Int.SIZE_BYTES
            Float::class -> Float.SIZE_BYTES
            else -> error("Cannot determine byte size for component type: $type")
        }
    }
    // @formatter:on

}

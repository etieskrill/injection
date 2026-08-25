package org.etieskrill.engine.graphics.shader

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import io.github.etieskrill.injection.extension.shader.Texture
import io.github.etieskrill.injection.extension.shader.Texture2D
import io.github.etieskrill.injection.extension.shader.Texture2DArray
import io.github.etieskrill.injection.extension.shader.Texture2DArrayShadow
import io.github.etieskrill.injection.extension.shader.Texture2DShadow
import io.github.etieskrill.injection.extension.shader.TextureCubeMap
import io.github.etieskrill.injection.extension.shader.TextureCubeMapArrayShadow
import io.github.etieskrill.injection.extension.shader.TextureCubeMapShadow
import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.joml.Matrix2f
import org.joml.Matrix2fc
import org.joml.Matrix3f
import org.joml.Matrix3fc
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

abstract class Shader protected constructor(
    internal val shaderFiles: List<String>,
    internal val strictUniformDetection: Boolean = true
) : AbstractShader {

    internal val uniforms = mutableMapOf<String, Uniform>()
    internal val uniformArrays = mutableMapOf<String, ArrayUniform>()
    internal var version: Long = 0L

    override fun setUniform(name: String, value: Any) {
        val uniform = uniforms[name]!! //TODO strict uniforms
        if (value::class != uniform.type.clazz || value::class != uniform.type.constClass) {
            logger.warn { "Tried setting uniform ${uniform.name} of type ${uniform.type} to incompatible value of type ${value::class.simpleName}" }
            return
        }

        if (uniform.value == value) return

        uniform.value = value
        uniform.version++
    }

    override fun setTexture(name: String, texture: Texture) {
        val uniform = uniforms[name]!! //TODO strict uniforms
        if (uniform.value == texture) return
        if (texture::class != uniform.type.clazz || texture::class != uniform.type.constClass) {
            logger.warn { "Tried setting texture ${uniform.name} of type ${uniform.type} to incompatible value of type ${texture::class.simpleName}" }
            return
        }

        uniform.value = texture
        uniform.version++
    }

    override fun addUniform(name: String, type: KClass<*>) {
        val uniformType = UniformType.from(type)
        if (uniformType == null) {
            logger.warn { "Failed to add uniform $name; type ${type.simpleName} is not a valid uniform type" }
            return
        }
        uniforms[name] = Uniform(name, uniformType)
        version++
    }

    override fun setUniformArray(name: String, value: Array<Any>) {
        val uniform = uniformArrays[name]!! //TODO strict uniforms
        if (value.isEmpty() || value[0]::class != uniform.elementType.clazz || value[0]::class != uniform.elementType.constClass) {
            logger.warn { "Tried setting uniform array ${uniform.name} with element type ${uniform.elementType} to incompatible value of type ${value[0]::class.simpleName}" }
            return
        }

        if (uniform.value.contentEquals(value)) return

        uniform.value = value
        uniform.version++
    }

    override fun setUniformArray(name: String, index: Int, value: Any) {
        val uniform = uniformArrays[name]!! //TODO strict uniforms
        if (index + 1 > uniform.size) {
            logger.warn { "Tried setting uniform array element at index $index when array only has ${uniform.size} elements" }
            return
        }
        if (value::class != uniform.elementType.clazz || value::class != uniform.elementType.constClass) {
            logger.warn { "Tried setting uniform array element ${uniform.name}[$index] with type ${uniform.elementType} to incompatible value of type ${value::class.simpleName}" }
            return
        }

        if (uniform.value?.get(index) == value) return

        uniform.value?.set(index, value)
        uniform.version++
    }

    override fun addUniformArray(name: String, size: Int, type: KClass<*>) {
        val uniformType = UniformType.from(type)
        if (uniformType == null) {
            logger.warn { "Failed to add uniform array $name; element type ${type.simpleName} is not a valid uniform type" }
            return
        }
        uniformArrays[name] = ArrayUniform(name, uniformType, size)
        version++
    }

    override fun setStorageBuffer(blockName: String, buffer: StorageBuffer<*>) {
        val uniform = uniforms[blockName]!! //TODO strict uniforms
        if (uniform.type != UniformType.STORAGE_BUFFER) {
            logger.warn { "Tried setting storage buffer $blockName when uniform type is ${uniform.type}" }
            return
        }

        if (uniform.value == buffer) return

        uniform.value = buffer
        uniform.version++
    }

    override fun addStorageBuffer(blockName: String, layout: BufferAccessor<*>) {
        uniforms[blockName] = Uniform(blockName, UniformType.STORAGE_BUFFER)
        version++
    }

}

expect class ShaderInstance : Disposable {
    val descriptor: Shader
    val context: GraphicsContext

    override fun dispose()
}

enum class ShaderType { VERTEX, GEOMETRY, FRAGMENT, COMPOSITE }

internal data class ShaderFile(
    val name: String,
    val type: ShaderType,
    val source: String
)

enum class UniformType(val glslName: String?, val clazz: KClass<*>, val constClass: KClass<*>? = null) {
    INT("int", Int::class),
    FLOAT("float", Float::class),
    DOUBLE("double", Double::class),
    BOOL("bool", Boolean::class),
    VEC2("vec2", Vector2f::class, Vector2fc::class),
    VEC2I("vec2i", Vector2i::class, Vector2ic::class),
    VEC3("vec3", Vector3f::class, Vector3fc::class),
    VEC4("vec4", Vector4f::class, Vector4fc::class),
    MAT2("mat2", Matrix2f::class, Matrix2fc::class),
    MAT3("mat3", Matrix3f::class, Matrix3fc::class),
    MAT4("mat4", Matrix4f::class, Matrix4fc::class),
    TEXTURE_2D("sampler2D", Texture2D::class),
    TEXTURE_2D_ARRAY("sampler2DArray", Texture2DArray::class),
    TEXTURE_2D_SHADOW("sampler2DShadow", Texture2DShadow::class),
    TEXTURE_2D_ARRAY_SHADOW("sampler2DArrayShadow", Texture2DArrayShadow::class),
    TEXTURE_CUBE_MAP("samplerCube", TextureCubeMap::class),
    TEXTURE_CUBE_MAP_ARRAY("samplerCubeArray", TextureCubeMap::class),
    TEXTURE_CUBE_MAP_SHADOW("samplerCubeShadow", TextureCubeMapShadow::class),
    TEXTURE_CUBE_MAP_ARRAY_SHADOW("samplerCubeArrayShadow", TextureCubeMapArrayShadow::class),
    STORAGE_BUFFER(null, StorageBuffer::class),
    STRUCT(null, UniformMappable::class);

    companion object {
        //TODO assignable instead of match?
        fun from(value: KClass<*>): UniformType? =
            entries.find { value::class == it.clazz || value::class == it.constClass }
    }
}

internal data class Uniform(
    val name: String,
    val type: UniformType
) {
    var value: Any? = null
    var version: Long = 0L
}

internal data class ArrayUniform(
    val name: String,
    val elementType: UniformType,
    val size: Int
) {
    var value: Array<Any>? = null
    var version: Long = 0L
}

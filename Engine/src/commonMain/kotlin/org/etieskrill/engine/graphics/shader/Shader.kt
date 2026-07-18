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

expect abstract class Shader : AbstractShader, Disposable {
    val context: GraphicsContext

    protected constructor(context: GraphicsContext, shaderFiles: List<String>, strictUniformDetection: Boolean = true)

    override fun setUniform(name: String, value: Any)
    override fun setTexture(name: String, texture: Texture)
    override fun addUniform(name: String, type: KClass<*>)

    override fun setUniformArray(name: String, value: Array<Any>)
    override fun setUniformArray(name: String, index: Int, value: Any)
    override fun addUniformArray(name: String, size: Int, type: KClass<*>)

    override fun setStorageBuffer(blockName: String, buffer: StorageBuffer<*>)
    override fun addStorageBuffer(blockName: String, layout: BufferAccessor<*>)

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
    STRUCT(null, UniformMappable::class)
}

internal expect open class Uniform {
    val name: String
    val type: UniformType
}

internal expect class ArrayUniform : Uniform {
    val size: Int
}

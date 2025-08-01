package io.github.etieskrill.injection.extension.shader

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
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.Vector4f
import org.joml.Vector4fc
import org.joml.Vector4i
import org.joml.Vector4ic

typealias int = Int
typealias float = Float
typealias double = Double
typealias bool = Boolean

typealias vec2 = Vector2fc
typealias vec3 = Vector3fc
typealias vec4 = Vector4fc

typealias ivec2 = Vector2ic
typealias ivec3 = Vector3ic
typealias ivec4 = Vector4ic

typealias mat2 = Matrix2fc
typealias mat3 = Matrix3fc
typealias mat4 = Matrix4fc

typealias sampler2D = Texture2D
typealias sampler2DArray = Texture2DArray
typealias samplerCube = TextureCubeMap
typealias samplerCubeArray = TextureCubeMapArray

typealias sampler2DShadow = Texture2DShadow
typealias sampler2DArrayShadow = Texture2DArrayShadow
typealias samplerCubeShadow = TextureCubeMapShadow
typealias samplerCubeArrayShadow = TextureCubeMapArrayShadow

typealias struct = Any

/**
 * Maps an [FQN][kotlin.reflect.KClass.qualifiedName] to it's GLSL counterpart, if it exists.
 * @return the GLSL equivalent of [this], or `null`
 */
val String.glslType: String? get() = primitiveTypes[jomlConstants[this] ?: this]

private val primitiveSamplerTypes = mapOf(
    sampler2D::class to "sampler2D",
    sampler2DArray::class to "sampler2DArray",
    samplerCube::class to "samplerCube",
    samplerCubeArray::class to "samplerCubeArray",
    sampler2DShadow::class to "sampler2DShadow",
    sampler2DArrayShadow::class to "sampler2DArrayShadow",
    samplerCubeShadow::class to "samplerCubeShadow",
    samplerCubeArrayShadow::class to "samplerCubeArrayShadow",
)

private val primitiveTypes = mutableMapOf(
    int::class to "int",
    float::class to "float",
    double::class to "double",
    bool::class to "bool",
    vec2::class to "vec2",
    ivec2::class to "ivec2",
    ivec3::class to "ivec3",
    ivec4::class to "ivec4",
    vec3::class to "vec3",
    vec4::class to "vec4",
    mat2::class to "mat2",
    mat3::class to "mat3",
    mat4::class to "mat4"
).apply { putAll(primitiveSamplerTypes) }.map { it.key.qualifiedName!! to it.value }.toMap()

private val jomlConstants = mapOf(
    Vector2f::class to Vector2fc::class,
    Vector2i::class to Vector2ic::class,
    Vector3f::class to Vector3fc::class,
    Vector3i::class to Vector3ic::class,
    Vector4f::class to Vector4fc::class,
    Vector4i::class to Vector4ic::class,
    Matrix2f::class to Matrix2fc::class,
    Matrix3f::class to Matrix3fc::class,
    Matrix4f::class to Matrix4fc::class
).map { it.key.qualifiedName!! to it.value.qualifiedName!! }.toMap()

package io.github.etieskrill.injection.extension.shader

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec3i
import net.bifreus.kml.Vec3ic
import net.bifreus.kml.Vec4i
import net.bifreus.kml.Vec4ic
import net.bifreus.kml.Mat2
import net.bifreus.kml.Mat2c
import net.bifreus.kml.Mat3
import net.bifreus.kml.Mat3c
import net.bifreus.kml.Mat4
import net.bifreus.kml.Mat4c

typealias int = Int
typealias float = Float
typealias double = Double
typealias bool = Boolean

typealias vec2 = Vec2c
typealias vec3 = Vec3c
typealias vec4 = Vec4c

typealias ivec2 = Vec2ic
typealias ivec3 = Vec3ic
typealias ivec4 = Vec4ic

typealias mat2 = Mat2c
typealias mat3 = Mat3c
typealias mat4 = Mat4c

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
    Vec2::class to Vec2c::class,
    Vec2i::class to Vec2ic::class,
    Vec3::class to Vec3c::class,
    Vec3i::class to Vec3ic::class,
    Vec4::class to Vec4c::class,
    Vec4i::class to Vec4ic::class,
    Mat2::class to Mat2c::class,
    Mat3::class to Mat3c::class,
    Mat4::class to Mat4c::class
).map { it.key.qualifiedName!! to it.value.qualifiedName!! }.toMap()

package io.github.etieskrill.injection.extension.shader

import org.joml.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias int = Int
typealias float = Float
typealias double = Double
typealias bool = Boolean

typealias vec2 = Vector2fc
typealias vec3 = Vector3fc
typealias vec4 = Vector4fc

typealias mat3 = Matrix3fc
typealias mat4 = Matrix4fc

typealias sampler2D = Texture2D
typealias sampler2DArray = Texture2DArray
typealias samplerCube = TextureCubeMap
typealias samplerCubeArray = TextureCubeMapArray

typealias sampler2DShadow = ShadowMap<sampler2D>
typealias sampler2DArrayShadow = ShadowMap<sampler2DArray>
typealias samplerCubeShadow = ShadowMap<samplerCube>
typealias samplerCubeArrayShadow = ShadowMap<samplerCubeArray>

typealias struct = Any

private val primitiveSamplerTypes = mapOf(
    sampler2D::class to "sampler2D",
    sampler2DArray::class to "sampler2DArray",
    samplerCube::class to "samplerCube",
    samplerCubeArray::class to "samplerCubeArray"
)

private val primitiveTypes = mutableMapOf(
    int::class to "int",
    float::class to "float",
    double::class to "double",
    bool::class to "bool",
    vec2::class to "vec2",
    vec3::class to "vec3",
    vec4::class to "vec4",
    mat3::class to "mat3",
    mat4::class to "mat4"
).apply { putAll(primitiveSamplerTypes) }.toMap()

private val jomlConstantTypes = mapOf(
    Vector2f::class to Vector2fc::class,
    Vector3f::class to Vector3fc::class,
    Vector4f::class to Vector4fc::class,
    Matrix3f::class to Matrix3fc::class,
    Matrix4f::class to Matrix4fc::class
)

private fun KClass<*>.toJomlConstantType() = when (this) {
    in jomlConstantTypes.keys -> jomlConstantTypes[this]!!
    else -> this
}

private val jomlConstantNames = jomlConstantTypes.map { it.key.qualifiedName to it.value.qualifiedName }.toMap()

private fun String.toJomlConstantType() = when (this) {
    in jomlConstantNames -> jomlConstantNames[this]!!
    else -> this
}

val KClass<*>.isGlslPrimitive: Boolean
    get() = when (this.toJomlConstantType()) {
        in primitiveTypes.keys -> true
        ShadowMap::class -> TODO("shadow types")
        else -> false
    }

val KClass<*>.glslName: String?
    get() = when (this.toJomlConstantType()) {
        in primitiveTypes.keys -> primitiveTypes[this]
        ShadowMap::class -> {
            val typeParams = findSuperclassTypeParameters<ShadowMap<*>>()
            check(typeParams.size == 1)

            val samplerType = primitiveSamplerTypes.firstNotNullOf {
                if (it.key.qualifiedName == typeParams[0].typeName) it.value else null
            }
            "${samplerType}Shadow"
        }

        else -> null
    }

val String.glslType: String?
    get() = primitiveTypes.firstNotNullOfOrNull {
        if (it.key.qualifiedName!! == this.toJomlConstantType()) it.value else null
    }

private inline fun <reified T : Any> KClass<*>.findShadowMapType() =
    T::class.java in findSuperclassTypeParameters<ShadowMap<*>>()

private inline fun <reified T : Any> KClass<*>.findSuperclassTypeParameters(): Array<Type> =
    this.java.genericInterfaces //kotlin seems to be a little behind java in terms of generic reflection
        .flatMap { listOf(it as? ParameterizedType ?: return@flatMap emptyList()) }
        .find { it.rawType == T::class.java }?.actualTypeArguments
        ?: emptyArray()

private fun <T : Any> KClass<*>.findSupertype(clazz: KClass<T>): KClass<T>? {
    if (this == clazz) return this as KClass<T>
    if (this == Any::class || !isSubclassOf(clazz)) return null
    supertypes.forEach { return@findSupertype (it.classifier as? KClass<*> ?: return@forEach).findSupertype(clazz) }
    return null
}

package io.github.etieskrill.injection.extension.shader

import org.joml.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias int = Int
typealias float = Float
typealias bool = Boolean

//FIXME used to be final versions (e.g. Vector2fc), changed for dsl module, warn to not directly modify, or add observable or something
typealias vec2 = Vector2f
typealias vec3 = Vector3f
typealias vec4 = Vector4f

typealias mat3 = Matrix3f
typealias mat4 = Matrix4f

typealias sampler2D = Texture2D
typealias sampler2DArray = Texture2DArray
typealias sampler2DShadow = ShadowMap<Texture2D>
typealias samplerCubeArrayShadow = ShadowMap<TextureCubeMapArray>

typealias struct = Any

val KClass<*>.glslName: String?
    get() = when (this) {
        Int::class -> "int"
        Float::class -> "float"
        Boolean::class -> "bool"
        Vector2f::class -> "vec2"
        Vector3f::class -> "vec3"
        Vector4f::class -> "vec4"
        Matrix3f::class -> "mat3"
        Matrix4f::class -> "mat4"
        Texture2D::class -> "sampler2D"
        Texture2DArray::class -> "sampler2DArray"
        else -> {
            if (findShadowMapType<Texture2D>()) "sampler2D"
            else if (findShadowMapType<TextureCubeMapArray>()) "samplerCubeMapArrayShadow"
            else null
        }
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

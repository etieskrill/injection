package io.github.etieskrill.injection.extension.shader

import kotlin.reflect.KClass

interface AbstractShader {

    fun setUniform(name: String, value: Any)
    fun setTexture(name: String, texture: Texture)
    fun addUniform(name: String, type: KClass<*>)

    fun setUniformArray(name: String, value: Array<Any>)
    fun setUniformArray(name: String, index: Int, value: Any)
    fun addUniformArray(name: String, size: Int, type: KClass<*>)

    fun setStorageBuffer(blockName: String, buffer: StorageBuffer<*>)
    fun addStorageBuffer(blockName: String, layout: BufferAccessor<*>)

    fun dispose()

}

enum class ShaderStage { NONE, VERTEX, GEOMETRY, FRAGMENT }

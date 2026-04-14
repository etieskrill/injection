package io.github.etieskrill.sandbox

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import io.github.etieskrill.injection.extension.shader.Texture
import io.github.etieskrill.injection.extension.shader.dsl.ColourBloomRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4

val dummyShaderClass = object : AbstractShader {
    override fun setUniform(name: String, value: Any) {
        TODO("Not yet implemented")
    }

    override fun setUniformArray(name: String, value: Array<Any>) {
        TODO("Not yet implemented")
    }

    override fun setUniformArray(name: String, index: Int, value: Any) {
        TODO("Not yet implemented")
    }

    override fun setTexture(name: String, texture: Texture) {
        TODO("Not yet implemented")
    }

    override fun setStorageBuffer(blockName: String, buffer: StorageBuffer<*>) {
        TODO("Not yet implemented")
    }

    override fun addUniform(name: String, type: Class<*>) {
        TODO("Not yet implemented")
    }

    override fun addUniformArray(name: String, size: Int, type: Class<*>) {
        TODO("Not yet implemented")
    }

    override fun addStorageBuffer(blockName: String, layout: BufferAccessor<*>) {
        TODO("Not yet implemented")
    }

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}

class SimpleTestShader : ShaderBuilder<Unit, VertexData, ColourBloomRenderTarget>(dummyShaderClass) {
    val model by uniform<mat4>()
    val combined by uniform<mat4>()

    override fun program() {
        vertex {
            VertexData(combined * model * vec4(0, 1, 2, 3))
        }
        fragment {
            ColourBloomRenderTarget(
                it.position.rt,
                vec4(0f, 0f, 0f, 1f).rt
            )
        }
    }
}

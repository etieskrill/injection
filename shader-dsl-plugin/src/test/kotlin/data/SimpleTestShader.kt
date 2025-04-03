package io.github.etieskrill.injection.extension.shader.dsl.data

import io.github.etieskrill.injection.extension.shader.dsl.*
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.joml.times

class Vertex(val position: vec3, val texCoord: vec2)
class VertexData(override val position: vec4, val texCoord: vec2) : ShaderVertexData
class RenderTargets(val colour: RenderTarget, val bloom: RenderTarget)

class SimpleTestShader : ShaderBuilder<Vertex, VertexData, RenderTargets>(Shader()) {
    var model by uniform<mat4>()
    var combined by uniform<mat4>()

    override fun program() {
        vertex {
            val position = this@SimpleTestShader.combined * this@SimpleTestShader.model * vec4(it.position, 1.0f)
            VertexData(
                position,
                it.texCoord
            )
        }
        fragment {
            RenderTargets(
                it.position.rt,
                vec4(0f, 0f, 0f, 1f).rt
            )
        }
    }
}

package org.etieskrill.engine

import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.joml.times

class Vertex(val position: vec3 = vec3(), val texCoord: vec2 = vec2())
class VertexData(val position: vec4 = vec4(), val texCoord: vec2 = vec2())
class RenderTargets(val colour: RenderTarget, val bloom: RenderTarget)

class TestShader : ShaderBuilder<Vertex, VertexData, RenderTargets>(TestShaderProgram()) {
    var model by uniform<mat4>()
    var combined by uniform<mat4>()

    override fun program() {
        vertex {
            val position = this@TestShader.combined * this@TestShader.model * vec4(it.position, 1.0f)
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

class TestShaderProgram : ShaderProgram(listOf("Test.glsl"))

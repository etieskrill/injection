package org.etieskrill.engine

import io.github.etieskrill.extension.shader.dsl.RenderTarget
import io.github.etieskrill.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.joml.times

//data class Vertex(val i: Int)
//data class VertexData(val i: Int)
//data class RenderTargets(val i: Int)

open class Vertex(open val position: vec3 = vec3(), open val texCoord: vec2 = vec2())
open class VertexData(open val position: vec4 = vec4(), open val texCoord: vec2 = vec2())
open class RenderTargets(open val colour: RenderTarget, open val bloom: RenderTarget)

class TestShader : ShaderBuilder<Vertex, VertexData, RenderTargets>(
    Vertex::class,
    VertexData::class,
    RenderTargets::class,
    TestShaderProgram()
) {
    var model by uniform<mat4>()
    var combined by uniform<mat4>()

//    init {
//        program()
//        check(callDepth == -1) { "Not all proxied function calls returned. This... should not happen." }
//        generateGlsl()
//    }

    override fun program() {
        vertex {
//            code("uniform vec3 sugondeez;")
//            code("${this@TestShader.combined} * ${this@TestShader.model} * sugondeez;")
            VertexData(
                this@TestShader.combined * this@TestShader.model * vec4(it.position, 1.0f),
//                code("combined * model * vec4(position, 1.0)"),
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

class TestShaderProgram : ShaderProgram(
    listOf("Test.glsl")
)
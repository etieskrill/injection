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

        /* is interpreted as:
        - attribute vec3 position
        - attribute vec2 texCoord

        - vertex field vec4 position
        - vertex field vec2 texCoord

        - render target vec4 bloom
        - render target vec4 colour

        - uniform mat4 matA //combine with first getter to get name, and also validate usage
        - uniform mat4 matB

        - combined // see above ^
        - model
        - matC = combined * model //btw these operations are supposedly right associative, so wtf is this
        - position
        - vecA = vec4(position, 1.0)
        - vecB = matC * vecA
        - texCoord
        - VertexData(vecB, texCoord)

        - position
        - renderTarget0 = *probably* position
        - vecC = vec4(0.0, 0.0, 0.0, 1.0)
        - renderTarget1 = *probably* vecC
        - RenderTargets(renderTarget0, renderTarget1)
         */

        /* may be compiled into:
        #version 330 core

        struct VertexData {
            vec4 position;
            vec2 texCoord;
        };

        uniform mat4 model;
        uniform mat4 combined;

        #pragma stage vertex

        layout (location = 0) in vec3 position;
        layout (location = 1) in vec2 texCoord;

        out VertexData vertex;

        void main() {
            vertex.position = combined * model * vec4(position, 1.0);
            vertex.texCoord = texCoord;
        }

        #pragma stage fragment

        in VertexData vertex;

        out vec4 colour;
        out vec4 bloom;

        void main() {
            colour = vertex.position;
            bloom = vec4(0.0, 0.0, 0.0, 1.0);
        }
         */
    }
}

class TestShaderProgram : ShaderProgram(
    listOf("Test.glsl")
)
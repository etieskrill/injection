package io.github.etieskrill.injection.extension.shader.dsl.data

import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.dsl.*

class HDRShader : ShaderBuilder<Any, HDRShader.Vertex, HDRShader.RenderTargets>(Shader()) {

    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData
    data class RenderTargets(val fragColour: RenderTarget)

    val someConst by const(5)
    val vertices by const(arrayOf<vec2>(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var hdrBuffer by uniform<sampler2D>()
    var exposure by uniform<float>()
    var reinhard by uniform<bool>()

    var bloomBuffer by uniform<sampler2D>()

    override fun program() {
        vertex {
            Vertex(
                position = vec4(this@HDRShader.vertices[vertexID], 0, 1),
                texCoords = max(this@HDRShader.vertices[vertexID], vec2(0, 0))
            )
        }
        fragment {
            RenderTargets(
                fragColour = vec4(1, 0, 0, 1).rt
            )
        }
    }

}

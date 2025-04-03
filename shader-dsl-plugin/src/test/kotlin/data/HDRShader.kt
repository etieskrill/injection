package io.github.etieskrill.injection.extension.shader.dsl.data

import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.dsl.*

class HDRShader : ShaderBuilder<Any, HDRShader.Vertex, HDRShader.RenderTargets>(Shader()) {

    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData
    data class RenderTargets(val fragColour: RenderTarget)

    val someConst by const(5)
    val anotherConst by const(vec2(-1, 1))
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

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
            var hdr = texture(this@HDRShader.hdrBuffer, it.texCoords).rgb
            val bloom = texture(this@HDRShader.bloomBuffer, it.texCoords).rgb
            hdr = bloom
            hdr = hdr + bloom
            hdr += bloom

            val mapped: vec3
            if (this@HDRShader.reinhard) {
                mapped = hdr / (hdr + vec3(1))
            } else {
                mapped = vec3(1) - exp(-hdr * this@HDRShader.exposure)
            }

            RenderTargets(
                fragColour = vec4(pow(mapped, vec3(1 / 2.2)), 1).rt
            )
        }
    }

}

package org.etieskrill.engine.entity.service.impl

import io.github.etieskrill.injection.extension.shader.bool
import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

class HDRShader : PureShaderBuilder<HDRShader.Vertex, ColourRenderTarget>(
    object : ShaderProgram(listOf("HDR.glsl")) {}
) {
    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData

    private val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var hdrBuffer by uniform<sampler2D>()
    var bloomBuffer by uniform<sampler2D>()
    var reinhard by uniform<bool>()
    var exposure by uniform<float>()

    init {
        exposure = 1f
        reinhard = true
    }

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
            hdr += bloom

            val mapped = if (this@HDRShader.reinhard) {
                hdr / (hdr + vec3(1))
            } else {
                vec3(1) - exp(-hdr * this@HDRShader.exposure)
            }

            val gammaCorrected = pow(mapped, vec3(1 / 2.2))

            ColourRenderTarget(colour = vec4(gammaCorrected, 1))
        }
    }
}

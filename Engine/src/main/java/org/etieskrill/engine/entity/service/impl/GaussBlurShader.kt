package org.etieskrill.engine.entity.service.impl

import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

class GaussBlurShader : ShaderBuilder<Any, GaussBlurShader.Vertex, GaussBlurShader.RenderTargets>(
    object : ShaderProgram(listOf("GaussBlur.glsl"), false) {}
) {
    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var source by uniform<sampler2D>()
    var horizontal by uniform<bool>()
    var sampleDistance by uniform<vec2>() //FIXME this is actually a pretty stupid idea as it introduces some ugly grid-like artifacts - use larger convolution mask instead

    init {
        sampleDistance = vec2(1, 1)
    }

    val weights by const(arrayOf(0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216))

    private fun sampleWithOffset(offset: vec2): vec3 = fragFunc {
        var result = vec3(0)
        for (i in 1..weights.size) {
            result += texture(source, it.texCoords + offset * sampleDistance * i).rgb * weights[i]
            result += texture(source, it.texCoords - offset * sampleDistance * i).rgb * weights[i]
        }
        result
    }

    override fun program() {
        vertex {
            Vertex(
                vec4(vertices[vertexID], 0, 1),
                max(vertices[vertexID], vec2(0, 0))
            )
        }
        fragment {
            val texOffset = 1.0 / vec2(textureSize(source, 0))
            var result = texture(source, it.texCoords).rgb * weights[0]

            val offset =
                if (horizontal) vec2(texOffset.x, 0)
                else vec2(0, texOffset.y)

            result += sampleWithOffset(offset)

            RenderTargets(
                colour = vec4(result, 1.0).rt
            )
        }
    }
}
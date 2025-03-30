package io.github.etieskrill.injection.extension.shader.dsl.data

import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.dsl.*

class GaussBlurShader : ShaderBuilder<Any, GaussBlurShader.Vertex, GaussBlurShader.RenderTargets>(
    Shader()
) {
    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    val source by uniform<sampler2D>()
    val horizontal by uniform<bool>()
    val sampleDistance by uniform<vec2>()

    val weights by const(arrayOf(0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216))

    private fun sampleWithOffset(offset: vec2): vec3 = func {
        var result = vec3(0)
        for (i in 0..weights.size) { //TODO omg i want to commit toaster bath
//        for (i in weights.indices) {
//        for ((i, weight) in weights.withIndex()) {
            result += texture(source, vertexData.texCoords + offset * sampleDistance * i).rgb * weights[i]
            result += texture(source, vertexData.texCoords - offset * sampleDistance * i).rgb * weights[i]
        }
        result
    }

    override fun program() {
        vertex {
//            vertices[0] <- FIXME - not really a usecase, so throw readable exception
            Vertex(
                vec4(vertices[vertexID], 0, 1),
                max(vertices[vertexID], vec2(0, 0))
            )
        }
        fragment {
            val texOffset = 1 / textureSize(source, 0)
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
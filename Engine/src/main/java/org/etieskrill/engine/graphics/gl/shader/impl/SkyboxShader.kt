package org.etieskrill.engine.graphics.gl.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.samplerCube
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.joml.div

class SkyboxShader : PureShaderBuilder<SkyboxShader.Vertex, SkyboxShader.RenderTargets>(
    object : ShaderProgram(listOf("Skybox.glsl")) {}
) {
    data class Vertex(override val position: vec4, val texCoord: vec3) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    private val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    private var combined by uniform<mat4>() //TODO check that uniforms are var
    private var skybox by uniform<samplerCube>()

    override fun program() {
        vertex {
            val ndc = vertices[vertexID]

            var nearPoint = inverse(combined) * vec4(ndc, -1, 1)
            var farPoint = inverse(combined) * vec4(ndc, 1, 1)

            nearPoint /= nearPoint.w
            farPoint /= farPoint.w

            val rayDirection = normalize(farPoint.xyz - nearPoint.xyz)

            Vertex(
                position = vec4(vertices[vertexID], 0, 1),
                texCoord = rayDirection
            )
        }
        fragment {
            RenderTargets((texture(skybox, it.texCoord)).rt)
        }
    }
}

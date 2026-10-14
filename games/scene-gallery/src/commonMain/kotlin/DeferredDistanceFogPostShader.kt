package net.bifreus.games.scene.gallery

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.std.rgb2vec3
import io.github.etieskrill.injection.extension.shader.sampler2D
import org.etieskrill.engine.graphics.shader.Shader

class DeferredDistanceFogPostShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : Shader(listOf("shaders/DeferredDistanceFogPost.glsl"), false) {}
) {
    private val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var screenTexture by uniform<sampler2D>()

    var gDepth: sampler2D by uniform()

    init {
        addUniform("screenTexture", sampler2D::class)
        addUniform("gDepth", sampler2D::class)
    }

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val texCoords = it.position.xy / 2.0 + 0.5

            val screenColour = texture(screenTexture, texCoords).rgb
            val depth = texture(gDepth, texCoords).r

            //FIXME un-hardcode & un-project from depth for true distance
            val linearDepth = 2.0 * ((1.0 / ((depth * ((1.0 / 100.0) - (1.0 / 0.1))) + (1.0 / 0.1))) - 1.0)
            val fogIntensity = min(0.95, max(0.0, (linearDepth - 1.0) / 25.0))
            val fogColour = rgb2vec3("2a3229")

            val fragColour = mix(screenColour, fogColour, fogIntensity)

            ColourRenderTarget(vec4(fragColour, 1.0))
        }
    }
}

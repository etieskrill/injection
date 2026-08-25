package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.Texture2D
import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.shader.Shader

class UiBoxShader : PureShaderBuilder<UiBoxShader.VertexData, ColourRenderTarget>(
    object : Shader(listOf("UiBox.glsl")) {}
) {
    data class VertexData(
        override val position: vec4,
        val textureCoords: vec2
    ) : ShaderVertexData

    val vertices by const(arrayOf(vec2(0, 0), vec2(1, 0), vec2(0, 1), vec2(1, 1)))

    var position by uniform<vec2>()
    var size by uniform<vec2>()

    var combined by uniform<mat4>()

    var useSprite by uniform<Boolean>()
    var sprite by uniform<Texture2D>()
    var colour by uniform<vec4>()

    override fun program() {
        vertex {
            VertexData(
                combined * vec4(position + (size * vertices[vertexID]), 0, 1),
                vertices[vertexID]
            )
        }
        fragment {
            val texel = if (useSprite) {
                texture(sprite, it.textureCoords)
            } else {
                vec4(1)
            }

            ColourRenderTarget(texel * colour)
        }
    }
}

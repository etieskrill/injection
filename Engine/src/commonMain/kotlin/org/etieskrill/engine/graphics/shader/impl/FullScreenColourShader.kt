package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.shader.Shader

class FullScreenColourShader(context: GraphicsContext) : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : Shader(context, listOf("FullScreenColour.glsl")) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var colour by uniform<vec4>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment { ColourRenderTarget(colour) }
    }
}
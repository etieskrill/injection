package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.shader.Shader

class ScreenSpacePointShader(context: GraphicsContext) : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : Shader(context, listOf("ScreenSpacePoint.glsl"), false) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var ndcPosition by uniform<vec2>()
    var aspectRatio by uniform<float>()
    var size by uniform<float>()
    var colour by uniform<vec4>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val distance = it.position.xy - ndcPosition
            distance.x *= aspectRatio
            val fragColour = if (length(distance) < size) colour else vec4(0)
            ColourRenderTarget(fragColour)
        }
    }
}
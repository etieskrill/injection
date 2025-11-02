package org.etieskrill.engine.graphics.animation

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

class UiOutlineShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("UiOutline.glsl"), false) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var position by uniform<vec2>()
    var size by uniform<vec2>()

    var backgroundColour by uniform<vec4>()

    var borderThickness by uniform<vec2>()
    var borderColour by uniform<vec4>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            if (it.position.x < position.x || it.position.y > position.y) discard()
            val endPosition = position + size
            if (it.position.x > endPosition.x || it.position.y < endPosition.y) discard()

            var fragColour = backgroundColour

            if (abs(it.position.x - position.x) < borderThickness.x
                || abs(it.position.y - position.y) < borderThickness.y
                || abs(it.position.x - endPosition.x) < borderThickness.x
                || abs(it.position.y - endPosition.y) < borderThickness.y
            ) {
                fragColour += borderColour
            }

            ColourRenderTarget(fragColour.rt)
        }
    }
}

package io.github.etieskrill.games.circles

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.int
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

class CircleShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Circle.glsl"), false) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var cursorPosition by uniform<vec2>()
    var size by uniform<float>()

    var combined by uniform<mat4>()
    var aspect by uniform<float>()

    companion object { //TODO enums would be pretty cool
        const val CIRCLE = 0
        const val FIRE_RUNE = 1
    }

    var shapeType by uniform<int>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            var distance: float
            if (shapeType == CIRCLE) { //FIXME for inlines, only single expressions work -> check if branch result is body/container, replace only return statements, otherwise no change
                val dist = vec2(it.position) - vec2(combined * vec4(cursorPosition, 0, 1))
                dist.y /= aspect
                distance = 1 - abs(length(dist) - size)
                distance *= distance * distance * distance * distance * distance * distance
            } else if (shapeType == FIRE_RUNE) {
                distance = 0.75f
            } else {
                distance = 0.0f
            }

            ColourRenderTarget(vec4(distance, distance, distance, 1.0).rt)
        }
    }
}

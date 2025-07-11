package io.github.etieskrill.games.circles

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.int
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.sampler2D
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

    fun sdfUnion(d1: float, d2: float) = func { min(d1, d2).toFloat() }
    fun sdfSubtract(d1: float, d2: float) = func { max(d1, -d2).toFloat() }
    fun sdfIntersect(d1: float, d2: float) = func { max(d1, d2).toFloat() }
    fun sdfXor(d1: float, d2: float) = func { max(min(d1, d2), -max(d1, d2)).toFloat() }

    fun sdfCircle(pos: vec2, size: float) = func { length(pos) - size }
    fun sdfTriangle(pos: vec2, size: float) = func { //i am 100% certain that i understand exactly 0% of how this works
        val q = abs(pos)
        val stuff = max(q.x * 0.866025 + pos.y * 0.5, -pos.y)
        max(-size, stuff - size * 0.5).toFloat()
    }

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val pos = vec2(it.position) - vec2(combined * vec4(cursorPosition, 0, 1))
            pos.y /= aspect

            val distance =
                if (shapeType == CIRCLE) { //FIXME for inlines, only single expressions work -> check if branch result is body/container, replace only return statements, otherwise no change
                    sdfSubtract(sdfCircle(pos, size), sdfCircle(pos, size * 0.975f))
            } else if (shapeType == FIRE_RUNE) {
                    sdfSubtract(sdfTriangle(pos, size), sdfTriangle(pos, size * 0.95f))
            } else {
                    0.0f
            }

//            val colour = exp(-distance) //FIXME oh bollocks these types of collisions exist too, of course
            val fragColour = exp(-50 * distance)

            ColourRenderTarget(vec4(fragColour, fragColour, fragColour, 1.0).rt)
        }
    }
}

class PaintShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Paint.glsl")) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var position by uniform<vec2>() //normalised plz
    var size by uniform<vec2>() //normalised plz

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val fragColour = 1 - length(vec2(it.position) - position) * size.x
            ColourRenderTarget(vec4(fragColour).rt)
        }
    }
}

class FullScreenTextureShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("FullScreenTexture.glsl")) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var sprite by uniform<sampler2D>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment { ColourRenderTarget(texture(sprite, vec2(it.position)).rt) }
    }
}

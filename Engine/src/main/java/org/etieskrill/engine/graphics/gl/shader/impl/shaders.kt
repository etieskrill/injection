package org.etieskrill.engine.graphics.gl.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.dsl.std.rotationMat2
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.mat3
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.joml.Vector2f
import org.joml.Vector4f

class BlitShader : PureShaderBuilder<BlitShader.Vertex, BlitShader.RenderTargets>(
    object : ShaderProgram(listOf("Blit.glsl")) {}
) {
    data class Vertex(override val position: vec4, val textureCoords: vec2) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var sprite by uniform<sampler2D>()

    var position by uniform<vec2>() //TODO either disable strict checking for builders, or do not compile
    var size by uniform<vec2>()
    var rotation by uniform<float>()

    var colour by uniform<vec4>()

    var windowSize by uniform<vec2>() //TODO check if used without being set

    init {
        position = Vector2f(0f)
        size = Vector2f(100f)
        rotation = 0f
        colour = Vector4f(1f)
    }

    override fun program() {
        vertex {
            var point = rotationMat2(rotation) * vertices[vertexID]
            point *= size
            point += 2 * vec2(position.x, -position.y) - vec2(-size.x, size.y)
            point /= windowSize
            point -= vec2(1, -1)
            Vertex(vec4(point, 0, 1), max(vec2(0), vertices[vertexID]))
        }
        fragment {
            val texel = vec4(colour.rgb, texture(sprite, it.textureCoords).a * colour.a)
            RenderTargets(texel.rt)
        }
    }
}

//TODO compile constants - e.g. colour blend mode to merge this with above
class BlitDepthShader : PureShaderBuilder<BlitDepthShader.Vertex, BlitDepthShader.RenderTargets>(
    object : ShaderProgram(listOf("BlitDepth.glsl")) {}
) {
    data class Vertex(override val position: vec4, val textureCoords: vec2) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var sprite by uniform<sampler2D>()

    var position by uniform<vec2>() //TODO either disable strict checking for builders, or do not compile
    var size by uniform<vec2>()
    var rotation by uniform<float>()

    var colour by uniform<vec4>()

    var windowSize by uniform<vec2>() //TODO check if used without being set

    init {
        position = Vector2f(0f)
        size = Vector2f(100f)
        rotation = 0f
        colour = Vector4f(1f)
    }

    override fun program() {
        vertex {
            var point = rotationMat2(rotation) * vertices[vertexID]
            point *= size
            point += 2 * vec2(position.x, -position.y) - vec2(-size.x, size.y)
            point /= windowSize
            point -= vec2(1, -1)
            Vertex(vec4(point, 0, 1), max(vec2(0), vertices[vertexID]))
        }
        fragment {
            val texel = texture(sprite, it.textureCoords)
            RenderTargets((texel * colour).rt)
        }
    }
}

class OutlineShader : ShaderBuilder<OutlineShader.Vertex, VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Outline.glsl")) {}
) {
    data class Vertex(val position: vec3, val normalVec: vec3)

    val model by uniform<mat4>()
    val normal by uniform<mat3>()
    val combined by uniform<mat4>()

    var colour by uniform<vec4>()
    var outlineFactor by uniform<float>()

    init {
        colour = Vector4f(1f, 0f, 0f, 1f)
        outlineFactor = 0.1f
    }

    override fun program() {
        vertex {
            VertexData(combined * (model * vec4(it.position + it.normalVec * outlineFactor, 1)))
        }
        fragment {
            ColourRenderTarget(colour.rt)
        }
    }
}

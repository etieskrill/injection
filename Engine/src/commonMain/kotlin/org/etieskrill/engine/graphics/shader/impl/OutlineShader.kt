package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.mat3
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Vector4f

class OutlineShader : ShaderBuilder<OutlineShader.Vertex, VertexData, ColourRenderTarget>(
    object : Shader(listOf("Outline.glsl")) {}
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
        fragment { ColourRenderTarget(colour) }
    }
}
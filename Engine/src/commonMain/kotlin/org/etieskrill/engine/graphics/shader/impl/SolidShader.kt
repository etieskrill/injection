package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.mat3
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Vector4f

class SolidShader : ShaderBuilder<SolidShader.InputVertex, SolidShader.Vertex, ColourRenderTarget>(
    object : Shader(listOf("shaders/Solid.glsl"), false) {}
) {
    data class InputVertex(val position: vec3, val normalVec: vec3)
    data class Vertex(override val position: vec4, val normal: vec3) : ShaderVertexData

    private val lightPos by const(vec3(0, 0, 0))

    var mesh by uniform<mat4>()
    var model by uniform<mat4>()
    var normal by uniform<mat3>()
    var combined by uniform<mat4>()
    var viewPosition by uniform<vec3>()

    var colour by uniform<vec4>()

    init {
        colour = Vector4f(0.6f, 0.6f, 0.6f, 1f)
    }

    override fun program() {
        vertex { Vertex(combined * (model * (mesh * vec4(it.position, 1))), normal * it.normalVec) }
        fragment {
            val diffuse = max(0, dot(it.normal, normalize(viewPosition - it.position.xyz)))
            val light = min(1, diffuse + 0.1)
            ColourRenderTarget(vec4(colour.rgb * light, colour.a))
        }
    }
}
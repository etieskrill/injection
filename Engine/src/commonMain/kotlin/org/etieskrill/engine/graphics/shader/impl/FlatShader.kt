package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Vector4f

class FlatShader : ShaderBuilder<FlatShader.InputVertex, VertexData, ColourRenderTarget>(
    object : Shader(listOf("Flat.glsl")) {}
) {
    data class InputVertex(val position: vec3, val normal: vec3)

    var model by uniform<mat4>()
    var combined by uniform<mat4>()

    var colour by uniform<vec4>()

    init {
        colour = Vector4f(0.6f, 0.6f, 0.6f, 1f)
    }

    override fun program() {
        vertex { VertexData(combined * (model * vec4(it.position, 1))) }
        fragment { ColourRenderTarget(colour) }
    }
}


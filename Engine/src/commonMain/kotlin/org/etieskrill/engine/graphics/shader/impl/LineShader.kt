package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Matrix4f

class LineShader(context: GraphicsContext) : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : Shader(context, listOf("Line.glsl")) {}
) {
    var pointA by uniform<vec3>()
    var pointB by uniform<vec3>()

    var colour by uniform<vec4>()

    var combined by uniform<mat4>()

    init {
        combined = Matrix4f()
    }

    override fun program() {
//        vertex { VertexData(vec4(if (vertexID == 0) pointA else pointB, 1)) } //FIXME ruh oh
        vertex {
            val point = if (vertexID == 0) pointA else pointB
            VertexData(combined * vec4(point, 1))
        }
        fragment { ColourRenderTarget(colour) }
    }
}
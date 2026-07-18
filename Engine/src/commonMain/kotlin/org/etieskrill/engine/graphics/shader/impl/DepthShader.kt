package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.mat4
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.model.Vertex
import org.etieskrill.engine.graphics.shader.Shader

class DepthShader(context: GraphicsContext) : ShaderBuilder<Vertex, VertexData, DepthShader.RenderTargets>(
    object : Shader(context, listOf("Depth.glsl")) {}
) {
    class RenderTargets()

    var model by uniform<mat4>()
    var combined by uniform<mat4>()

    override fun program() {
        vertex { VertexData(combined * model * vec4(it.position, 1.0)) }
        fragment { RenderTargets() } //TODO texture transparency
    }
}

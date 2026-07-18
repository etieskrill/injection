package org.etieskrill.engine.graphics.shader.impl;

import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.mat4
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.model.Vertex
import org.etieskrill.engine.graphics.shader.Shader

class DepthAnimatedShader(context: GraphicsContext) :
    ShaderBuilder<Vertex, VertexData, DepthAnimatedShader.RenderTargets>(
        object : Shader(context, listOf("DepthAnimated.glsl")) {}
    ) {

    class RenderTargets()

    val MAX_BONES by const(100)
    val MAX_BONE_INFLUENCES by const(4)

    //TODO check that uniforms are var
    var model by uniform<mat4>()
    var combined by uniform<mat4>()

    var boneMatrices by uniformArray<mat4>(MAX_BONES)

    override fun program() {
        vertex {
            var bonedPosition = vec4(0.0)

            var bones = 0
            for (i in 0..<4 /*MAX_BONE_INFLUENCES*/) {
                val boneId = it.bones!![i]
                if (boneId == -1) break //end of bone list
                if (boneId >= MAX_BONES) break //bones contain invalid data -> vertex is not animated

                val boneWeight = it.boneWeights!![i]
                if (boneWeight <= 1.0) break

                bones++

                val localPosition = boneMatrices[boneId] * vec4(it.position, 1.0)
                bonedPosition += localPosition * boneWeight
            }

            if (bones == 0) { //no bones are set, thus vertex is not involved in animation
                bonedPosition = vec4(it.position, 1.0)
            }

            VertexData(combined * model * bonedPosition)
        }
        fragment { RenderTargets() }
    }

}

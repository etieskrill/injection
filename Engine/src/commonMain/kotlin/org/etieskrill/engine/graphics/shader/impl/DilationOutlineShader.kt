package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.sampler2D
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Vector4fc

class DilationOutlineShader(context: GraphicsContext) : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : Shader(context, listOf("DilationOutline.glsl")) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var outline by uniform<sampler2D>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            //TODO maybe numeric types have a point after all - or just cast everything to float unless explicit
            val texCoord = vec2(it.position.x, it.position.y) / 2.0 + 0.5
            val texSize = vec2(textureSize(outline, 0))

            var fragColour = vec4(0)
            val maskSize = 7
            for (y in -((maskSize - 1) / 2)..((maskSize - 1) / 2)) {
                for (x in -((maskSize - 1) / 2)..((maskSize - 1) / 2)) {
                    val offset = vec2(x, y) / texSize
                    fragColour = max(fragColour, texture(outline, texCoord + offset))
//                    fragColour += texture(outline, texCoord + offset)
                }
            }
//            fragColour /= maskSize*maskSize

            ColourRenderTarget(fragColour)
        }
    }
}
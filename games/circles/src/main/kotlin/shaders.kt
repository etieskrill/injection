package io.github.etieskrill.games.circles

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.int
import io.github.etieskrill.injection.extension.shader.ivec2
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.joml.Vector2f

class SDFShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("SDF.glsl"), false) {}
) {
    data class SDFVertex(//TODO const size array fields
        val position: vec2,
        val rotation: float,
        val size: float,
        val thickness: float, //only when shapeType == STYLE_LINE
        val glowStrength: float, //analytical (probably?) exponential glow
        private val _padding: vec2 = Vector2f(0f),
        val colour: vec4,
        val shapeType: int,
        val styleType: int,
        val layerType: int,
        val blendStrength: float //for sdf op smoothing
    )

    object SDFVertexAccessor : VertexArrayAccessor<SDFVertex>() { //TODO this... really needs to be automatic
        override fun registerFields() {
            addField<vec2> { vertex, buffer -> vertex.position.get(buffer) }
            addField<float> { vertex, buffer -> buffer.putFloat(vertex.rotation) }
            addField<float> { vertex, buffer -> buffer.putFloat(vertex.size) }
            addField<float> { vertex, buffer -> buffer.putFloat(vertex.thickness) }
            addField<float> { vertex, buffer -> buffer.putFloat(vertex.glowStrength) }
            addField<vec2> { vertex, buffer -> buffer.putFloat(0f).putFloat(0f) } //padding
            addField<vec4> { vertex, buffer -> vertex.colour.get(buffer) }
            addField<int> { vertex, buffer -> buffer.putInt(vertex.shapeType) }
            addField<int> { vertex, buffer -> buffer.putInt(vertex.styleType) }
            addField<int> { vertex, buffer -> buffer.putInt(vertex.layerType) }
            addField<float> { vertex, buffer -> buffer.putFloat(vertex.blendStrength) }
        }

        private inline fun <reified T> addField(mapper: FieldMapper<SDFVertex>) = addField(T::class.java, mapper)
    }

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var sdfLayers by storageBuffer<SDFVertex>()

    var combined by uniform<mat4>()
    var aspect by uniform<float>()

    companion object { //TODO enums would be pretty cool
        const val SHAPE_CIRCLE = 0
        const val SHAPE_TRIANGLE = 1
        const val SHAPE_TEXTURE = 31

        const val STYLE_FILLED = 0
        const val STYLE_LINE = 1

        const val LAYER_SET = 0
        const val LAYER_ADD = 1
        const val LAYER_SUB = 2
        const val LAYER_AND = 3
        const val LAYER_XOR = 4
        const val LAYER_SMOOTH_ADD = 5
        const val LAYER_SMOOTH_SUB = 6
        const val LAYER_SMOOTH_AND = 7
    }

    fun sdfOpUnion(a: float, b: float) = func { min(a, b).toFloat() }
    fun sdfOpSubtract(a: float, b: float) = func { max(a, -b).toFloat() }
    fun sdfOpIntersect(a: float, b: float) = func { max(a, b).toFloat() }
    fun sdfOpXor(a: float, b: float) = func { max(min(a, b), -max(a, b)).toFloat() }
    fun sdfOpSmoothUnion(a: float, b: float, blendStrength: float) =
        func { //TODO check for var redefinitions (here it was param k and value k)
            val k = blendStrength * 4f
            val h = max(k - abs(a - b), 0f)
            (min(a, b) - h * h * 0.25f / k).toFloat()
        }

    fun sdfOpSmoothSubtraction(a: float, b: float, blendStrength: float) = func {
//        return -opSmoothUnion(a, -b, k)
        val k = blendStrength * 4f
        val h = max(k - abs(-a - b), 0f)
        max(-a, b) + h * h * 0.25f / k
    }

    fun sdfOpSmoothIntersection(a: float, b: float, blendStrength: float) = func {
//        return -opSmoothUnion(-a, -b, k)
        val k = blendStrength * 4f
        val h = max(k - abs(a - b), 0f)
        max(a, b) + h * h * 0.25f / k
    }

    fun sdfShapeCircle(pos: vec2, size: float) = func { length(pos) - size }
    fun sdfShapeTriangle(pos: vec2, size: float) =
        func { //i am 100% certain that i understand exactly 0% of how this works
            val q = abs(pos)
            val stuff = max(q.x * 0.866025 + pos.y * 0.5, -pos.y)
            max(-size, stuff - size * 0.5).toFloat()
        }

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment { vertex ->
            var fragColour = vec4(0)
            for (sdf in sdfLayers) {
                val pos = vec2(vertex.position) - vec2(combined * vec4(sdf.position, 0, 1))
                pos.y /= aspect

                val distance =
                    if (sdf.shapeType == SHAPE_CIRCLE) { //FIXME for inlines, only single expressions work -> check if branch result is body/container, replace only return statements, otherwise no change
                        if (sdf.styleType == STYLE_FILLED) {
                            sdfShapeCircle(pos, sdf.size)
                        } else if (sdf.styleType == STYLE_LINE) {
                            sdfOpSubtract(sdfShapeCircle(pos, sdf.size), sdfShapeCircle(pos, sdf.size * (1f - sdf.thickness))) //0.975f
                        } else {
                            0f
                        } //TODO render error?
                    } else if (sdf.shapeType == SHAPE_TRIANGLE) {
                        sdfOpSubtract(sdfShapeTriangle(pos, sdf.size), sdfShapeTriangle(pos, sdf.size * (1f - sdf.thickness))) //0.95f
                    } else {
                        0.0f
                    }

                fragColour += sdf.colour * exp(-sdf.glowStrength * distance) //-50
            }

            ColourRenderTarget(fragColour.rt)
        }
    }
}

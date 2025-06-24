package org.etieskrill.engine.scene.component.plot

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.int
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.scene.component.Node
import org.etieskrill.engine.util.FixedArrayDeque
import org.joml.Vector3f
import org.joml.Vector4f

enum class HistogramScaleMode { FIXED, MAX_VALUE }

class Histogram(
    private val columns: Int,
    private val scaleMode: HistogramScaleMode = HistogramScaleMode.MAX_VALUE,
    private val maxValue: Float = 0f,
    private val backgroundColour: Vector4f = Vector4f(0f, 0f, 0f, 1f),
    private val drawSeparators: Boolean = true,
    private val separatorColour: Vector4f = Vector4f(0.15f, 0.15f, 0.15f, 1f),
    private val barColour: Vector4f = Vector4f(0f, 1f, 0f, 1f)
) : Node<Histogram>() {

    companion object { //TODO replace with specialisation parameters
        const val MAX_NUM_COLUMNS = 512
    }

    init {
        require(columns <= MAX_NUM_COLUMNS) {
            "Number of histogram columns must be smaller than or equal to $MAX_NUM_COLUMNS columns"
        }
    }

    val values: FixedArrayDeque<Float> = FixedArrayDeque(columns)
    private val arrayValues: Array<Float> = Array(columns) { 0.0f }

    private val pipeline = PostPassPipeline(HistogramShader(), null, depthTest = false)

    override fun render(batch: Batch) {
        val position = absolutePosition

        batch.renderBox(Vector3f(position, 0f), Vector3f(size, 0f), backgroundColour)

        val maxValue = values.max()

        values.forEachIndexed { index, value ->
            arrayValues[arrayValues.size - 1 - index] = when (scaleMode) {
                HistogramScaleMode.FIXED -> value / this.maxValue
                HistogramScaleMode.MAX_VALUE -> maxValue / maxValue
            }
        }
        pipeline.shader.apply {
            values = arrayValues
            numValues = columns
            this.position = position
            size = this@Histogram.size
            combined = batch.combined
            barColour = this@Histogram.barColour
        }
        batch.render(pipeline)

        if (drawSeparators) { //TODO this can be integrated into the shader too
            val columnWidth = size.x / columns

            for (column in 0..columns) {
                batch.renderBox(
                    Vector3f(position.x + column * columnWidth, position.y, 0f),
                    Vector3f(1f, size.y, 0f),
                    separatorColour
                )
            }
        }
    }

}

//TODO specialisation parameters on construction
class HistogramShader/*(columns: Int)*/ : PureShaderBuilder<HistogramShader.VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Histogram.glsl"), false) {}
) {
    data class VertexData(override val position: vec4, val worldPosition: vec4) : ShaderVertexData

    private val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var combined by uniform<mat4>()

    var position by uniform<vec2>()
    var size by uniform<vec2>()

    var values by uniformArray<float>(Histogram.MAX_NUM_COLUMNS)
    var numValues by uniform<int>()

    var barColour by uniform<vec4>()

    override fun program() {
        vertex {
            val ndc = vec4(vertices[vertexID], 0, 1)
            VertexData(ndc, inverse(combined) * ndc)
        }
        fragment {
            val valueIndex = int(floor(((it.worldPosition.x - position.x) / size.x) * numValues))
            val normalisedHeight = (it.worldPosition.y - position.y) / size.y
            if (!(it.worldPosition.x > position.x && it.worldPosition.x < position.x + size.x
                        && it.worldPosition.y > position.y && it.worldPosition.y < position.y + size.y
                        && values[valueIndex] < normalisedHeight)
            ) {
                discard()
            }
            ColourRenderTarget(barColour.rt)
        }
    }
}

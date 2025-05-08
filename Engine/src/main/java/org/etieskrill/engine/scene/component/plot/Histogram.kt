package org.etieskrill.engine.scene.component.plot

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
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
    private val separatorColour: Vector4f = Vector4f(0.15f, 0.15f, 0.15f, 1f),
    private val barColour: Vector4f = Vector4f(0f, 1f, 0f, 1f)
) : Node() {

    val values: FixedArrayDeque<Float> = FixedArrayDeque(columns)

    override fun render(batch: Batch) {
        batch.renderBox(Vector3f(position, 0f), Vector3f(size, 0f), backgroundColour)

        val maxValue = values.max()

        val columnWidth = size.x / columns
        for ((column, value) in values.withIndex()) {
            val columnHeight = when (scaleMode) {
                HistogramScaleMode.FIXED -> value / this.maxValue
                HistogramScaleMode.MAX_VALUE -> value / maxValue
            }
            batch.renderBox( //FIXME this is a little too immediate mode. actually do batching, or just write shader
                Vector3f((columns - column - 1) * columnWidth, this.size.y * (1 - columnHeight), 0f),
                Vector3f(columnWidth, size.y * columnHeight, 0f),
                barColour
            )
        }
        for (column in 0..columns) {
            batch.renderBox(
                Vector3f(column * columnWidth, 0f, 0f),
                Vector3f(1f, size.y, 0f),
                separatorColour
            )
        }
    }

    override fun hit(button: Key?, action: Keys.Action?, posX: Double, posY: Double): Boolean = false
}

package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.joml.Vector3f
import org.joml.Vector4f

class Slider(
    var value: Float,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float = 0f,
    val barColour: Vector4f = Vector4f(0f, 1f, 0f, 1f),
    var action: (t: Float) -> Unit = {}
) : Node<Slider>() {

    companion object {
        private const val BAR_MARGIN = 2f
    }

    init {
        value = value.coerceIn(minValue, maxValue)
        if (stepSize > 0) value -= value % stepSize
    }

    override fun render(batch: Batch) {
        batch.renderBox(Vector3f(absolutePosition, 0f), Vector3f(size, 0f), renderedColour)
        batch.renderBox(
            Vector3f(absolutePosition, 0f).add(BAR_MARGIN, BAR_MARGIN, 0f),
            Vector3f(size, 0f).sub(2 * BAR_MARGIN, 2 * BAR_MARGIN, 0f).mul(value / (maxValue - minValue), 1f, 1f),
            barColour
        )
    }

    override fun hit(button: Key?, action: Keys.Action?, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false

        if (button == Keys.LEFT_MOUSE.input && action == Keys.Action.RELEASE) {
            val t = (posX - absolutePosition.x).toFloat() / size.x
            value = t * (maxValue - minValue) + minValue
            if (stepSize > 0) value -= value % stepSize
            action(value)
            return true
        }

        return false
    }

    //TODO drag gesture

}

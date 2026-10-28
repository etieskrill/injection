package org.etieskrill.engine.scene.element

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec4
import net.bifreus.kml.plus
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node

class Slider(
    var value: Float,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float = 0f,
    val barColour: Vec4 = Vec4(0f, 1f, 0f, 1f),
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
        batch.renderBox(absolutePosition, size, renderedColour)
        batch.renderBox(
            Vec2(BAR_MARGIN) + absolutePosition,
            ((-2 * BAR_MARGIN) + size).apply { x *= value / (maxValue - minValue) },
            barColour
        )
    }

    override fun handleHit(event: KeyEvent, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false

        if (event.key == Key.LEFT_MOUSE && event.action == KeyEventAction.RELEASE) {
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
package org.etieskrill.engine.input.controller

import net.bifreus.kml.Vec2
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.input.CursorInputAdapter

class CursorCameraController(
    private val camera: Camera,

    var lookSensitivity: Double = 0.05, //just some values set by testing on two local devices (using mouse, not trackpad)
    var zoomSensitivity: Double = 0.5,

    var updateCondition: (() -> Boolean)? = null,
) : CursorInputAdapter {

    private val previousPosition = Vec2()
    private var firstValueSet = false

    private var enabled = true

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        if (!enabled) return false

        if (!firstValueSet) {
            previousPosition(posX, posY)
            firstValueSet = true
        }

        if (shouldUpdate()) {
            previousPosition.x -= posX.toFloat()
            previousPosition.y -= posY.toFloat()
            camera.rotate(
                (lookSensitivity * -previousPosition.y).toFloat(),
                (lookSensitivity * previousPosition.x).toFloat(),
                0f
            )
        }

        previousPosition(posX, posY)
        return true
    }

    override fun invokeScroll(deltaX: Double, deltaY: Double): Boolean {
        if (!enabled) return false

        val zoom = camera.zoom - deltaY * zoomSensitivity
        if (shouldUpdate()) camera.zoom = zoom.toFloat()
        return true
    }

    fun enable() {
        this.enabled = true
        this.firstValueSet = false
    }

    fun disable() {
        this.enabled = false
    }

    private fun shouldUpdate() = updateCondition?.let { it() } ?: true

}

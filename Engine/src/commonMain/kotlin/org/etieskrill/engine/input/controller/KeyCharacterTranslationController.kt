package org.etieskrill.engine.input.controller

import org.etieskrill.engine.graphics.camera.Camera
import org.joml.Math.toRadians
import org.joml.Vector3f
import org.joml.times

class KeyCharacterTranslationController(
    translation: Vector3f,
    camera: Camera,
    fixUpDirection: Boolean = true
) : KeyCharacterController<Vector3f>(
    translation, { delta, target, deltaPosition, speed ->
        if (!deltaPosition.equals(0f, 0f, 0f)) {
            deltaPosition.normalize()
        }

        val deltaTranslation = if (fixUpDirection) {
            deltaPosition.x = -deltaPosition.x
            deltaPosition.rotateY(toRadians(camera.yaw))
        } else {
            camera.relativeTranslation(deltaPosition)
        }

        translation.set(deltaTranslation * speed)
    }
)

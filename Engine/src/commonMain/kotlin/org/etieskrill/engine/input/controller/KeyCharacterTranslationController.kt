package org.etieskrill.engine.input.controller

import net.bifreus.kml.Vec3
import net.bifreus.kml.deg
import org.etieskrill.engine.graphics.camera.Camera

class KeyCharacterTranslationController(
    translation: Vec3,
    camera: Camera,
    fixUpDirection: Boolean = true
) : KeyCharacterController<Vec3>(
    translation, { delta, target, deltaPosition, speed ->
        if (!deltaPosition.equals(0f, 0f, 0f)) {
            deltaPosition.normalize()
        }

        val deltaTranslation = if (fixUpDirection) {
            deltaPosition(deltaPosition.rotate(y = camera.yaw.deg))
        } else {
            camera.relativeTranslation(deltaPosition)
        }

        translation(deltaTranslation * speed)
    }
)

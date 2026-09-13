package org.etieskrill.engine.input.controller

import org.etieskrill.engine.graphics.camera.Camera
import org.joml.times

class KeyCameraController(
    camera: Camera
) : KeyCharacterController<Camera>(camera, { delta, camera, deltaPosition, speed ->
    deltaPosition.x = -deltaPosition.x
    camera.translate(deltaPosition * delta.toFloat() * speed)
})

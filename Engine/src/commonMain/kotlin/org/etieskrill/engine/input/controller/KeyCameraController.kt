package org.etieskrill.engine.input.controller

import org.etieskrill.engine.graphics.camera.Camera

class KeyCameraController(
    camera: Camera
) : KeyCharacterController<Camera>(camera, { delta, camera, deltaPosition, speed ->
    camera.translate(deltaPosition * delta.toFloat() * speed)
})

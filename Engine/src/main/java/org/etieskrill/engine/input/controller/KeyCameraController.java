package org.etieskrill.engine.input.controller;

import org.etieskrill.engine.graphics.camera.Camera;

public class KeyCameraController extends KeyCharacterController<Camera> {

    public KeyCameraController(Camera target) {
        super(target, (delta, camera, deltaPosition, speed) -> {
            deltaPosition.x *= -1;
            camera.translate(deltaPosition.mul((float) (delta * speed)));
        });
    }

}

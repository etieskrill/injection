package org.etieskrill.engine.input.controller;

import org.etieskrill.engine.graphics.camera.Camera;
import org.joml.Vector3f;

import static java.lang.Math.toRadians;

public class KeyCharacterTranslationController extends KeyCharacterController<Vector3f> {

    public KeyCharacterTranslationController(
            Vector3f translation,
            Camera camera
    ) {
        super(translation, (delta, target, deltaPosition, speed) -> {
            if (!deltaPosition.equals(0, 0, 0)) {
                deltaPosition.normalize();
            }
            deltaPosition.rotateY((float) toRadians(camera.getYaw()));
            translation.set(deltaPosition.mul(speed));
        });
    }

}

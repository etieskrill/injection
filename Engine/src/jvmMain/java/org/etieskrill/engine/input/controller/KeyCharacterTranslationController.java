package org.etieskrill.engine.input.controller;

import org.etieskrill.engine.graphics.camera.Camera;
import org.joml.Vector3f;

import static java.lang.Math.toRadians;

public class KeyCharacterTranslationController extends KeyCharacterController<Vector3f> {

    public KeyCharacterTranslationController(
            Vector3f translation,
            Camera camera
    ) {
        this(translation, camera, true);
    }

    public KeyCharacterTranslationController(
            Vector3f translation,
            Camera camera,
            boolean fixUpDirection
    ) {
        super(translation, (delta, target, deltaPosition, speed) -> {
            if (!deltaPosition.equals(0, 0, 0)) {
                deltaPosition.normalize();
            }
            if (fixUpDirection) {
                deltaPosition.x = -deltaPosition.x;
                deltaPosition.rotateY((float) toRadians(camera.getYaw()));
            } else {
                deltaPosition = camera.relativeTranslation(deltaPosition);
            }
            translation.set(deltaPosition.mul(speed));
        });
    }

}

package org.etieskrill.engine.input.controller;

import org.etieskrill.engine.graphics.camera.Camera;
import org.joml.Vector3f;

public class KeyCameraTranslationController extends KeyCameraController {

    private final Vector3f translation;

    public KeyCameraTranslationController(Vector3f transform, Camera camera) {
        super(camera);
        this.translation = transform;
    }

    public KeyCameraTranslationController(Vector3f transform, Camera camera, float speed) {
        super(camera, speed);
        this.translation = transform;
    }

    @Override
    public void update(double delta) {
        updateInputManager(delta);

        if (updateCondition == null || updateCondition.get()) {
            if (!deltaPosition.equals(0, 0, 0)) deltaPosition.normalize();
            translation.set(camera.relativeTranslation(deltaPosition.mul(speed)));
        }
        deltaPosition.zero();
    }

}

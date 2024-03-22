package org.etieskrill.engine.input.controller;

import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.InputBinding;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class KeyCameraController extends KeyInputManager {

    private final Camera camera;
    private float speed;
    private @Nullable Supplier<Boolean> updateCondition;

    private final Vector3f deltaPosition;

    public KeyCameraController(Camera camera) {
        this(camera, 1);
    }

    public KeyCameraController(Camera camera, float speed) {
        this.camera = camera;
        this.speed = speed;

        this.deltaPosition = new Vector3f();

        addBindings(Input.bind(Keys.W).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, 0, 1)));
        addBindings(Input.bind(Keys.S).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, 0, -1)));
        addBindings(Input.bind(Keys.A).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(-1, 0, 0)));
        addBindings(Input.bind(Keys.D).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(1, 0, 0)));
        addBindings(Input.bind(Keys.SPACE).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, -1, 0)));
        addBindings(Input.bind(Keys.SHIFT).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, 1, 0)));
    }

    @Override
    public void update(double delta) {
        super.update(delta);

        if (updateCondition == null || updateCondition.get()) {
            camera.translate(deltaPosition.mul((float) (delta * speed)));
        }
        deltaPosition.zero();
    }

    public float getSpeed() {
        return speed;
    }

    public KeyCameraController setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public KeyCameraController setUpdateCondition(@NotNull Supplier<Boolean> updateCondition) {
        this.updateCondition = updateCondition;
        return this;
    }

}

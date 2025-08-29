package org.etieskrill.engine.input.controller;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.input.CursorInputAdapter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.function.Supplier;

public class CursorCameraController implements CursorInputAdapter {

    private final Camera camera;

    private @Getter
    @Setter double lookSensitivity;
    private @Getter
    @Setter double zoomSensitivity;

    private @Nullable Supplier<Boolean> updateCondition;

    private final Vector2d previousPosition;

    private boolean firstValueSet;

    private boolean enabled = true;

    public CursorCameraController(Camera camera) {
        this(camera, .05, .5); //just some values set by testing on two local devices (using mouse, not trackpad)
    }

    public CursorCameraController(Camera camera, double lookSensitivity, double zoomSensitivity) {
        this.camera = camera;
        this.lookSensitivity = lookSensitivity;
        this.zoomSensitivity = zoomSensitivity;

        this.previousPosition = new Vector2d();

        this.firstValueSet = false;
    }

    @Override
    public boolean invokeMove(double posX, double posY) {
        if (!enabled) return false;

        if (!firstValueSet) {
            previousPosition.set(posX, posY);
            firstValueSet = true;
        }

        previousPosition.sub(posX, posY);
        if (shouldUpdate())
            camera.rotate(
                    (float) (lookSensitivity * -previousPosition.y()),
                    (float) (lookSensitivity * previousPosition.x()),
                    0);

        previousPosition.set(posX, posY);
        return true;
    }

    @Override
    public boolean invokeScroll(double deltaX, double deltaY) {
        if (!enabled) return false;

        double zoom = camera.getZoom() - deltaY * zoomSensitivity;
        if (shouldUpdate()) camera.setZoom((float) zoom);
        return true;
    }

    public CursorCameraController setUpdateCondition(@Nullable Supplier<Boolean> updateCondition) {
        this.updateCondition = updateCondition;
        return this;
    }

    public void enable() {
        this.enabled = true;
        this.firstValueSet = false;
    }

    public void disable() {
        this.enabled = false;
    }

    private boolean shouldUpdate() {
        return updateCondition == null || updateCondition.get();
    }

}

package org.etieskrill.engine.input;

import org.etieskrill.engine.graphics.Camera;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CursorCameraController implements CursorInputAdapter {

    private final Camera camera;

    private double lookSensitivity;
    private double zoomSensitivity;

    private @Nullable Supplier<Boolean> updateCondition;

    private double previousX, previousY;

    private boolean firstValueSet;

    public CursorCameraController(Camera camera) {
        this(camera, .05, .5); //just some values set by testing on two local devices (using mouse, not trackpad)
    }

    public CursorCameraController(Camera camera, double lookSensitivity, double zoomSensitivity) {
        this.camera = camera;
        this.lookSensitivity = lookSensitivity;
        this.zoomSensitivity = zoomSensitivity;
        this.firstValueSet = false;
    }

    @Override
    public boolean invokeMove(double posX, double posY) {
        if (!firstValueSet) {
            previousX = posX;
            previousY = posY;
            firstValueSet = true;
        }

        double dX = previousX - posX, dY = previousY - posY;
        previousX = posX;
        previousY = posY;

        if (shouldUpdate()) camera.orient(lookSensitivity * -dY, lookSensitivity * dX, 0);
        return true;
    }

    @Override
    public boolean invokeScroll(double deltaX, double deltaY) {
        double zoom = camera.getZoom() - deltaY * zoomSensitivity;
        if (shouldUpdate()) camera.setZoom((float) zoom);
        return true;
    }

    public double getLookSensitivity() {
        return lookSensitivity;
    }

    public void setLookSensitivity(double lookSensitivity) {
        this.lookSensitivity = lookSensitivity;
    }

    public double getZoomSensitivity() {
        return zoomSensitivity;
    }

    public void setZoomSensitivity(double zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }

    public CursorCameraController setUpdateCondition(@Nullable Supplier<Boolean> updateCondition) {
        this.updateCondition = updateCondition;
        return this;
    }

    private boolean shouldUpdate() {
        return updateCondition == null || updateCondition.get();
    }

}

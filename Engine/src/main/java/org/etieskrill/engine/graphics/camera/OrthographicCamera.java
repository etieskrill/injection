package org.etieskrill.engine.graphics.camera;

import org.joml.Vector2ic;

public class OrthographicCamera extends Camera {

    private float top, bottom, left, right;
    private final boolean manualViewportSize;

    public OrthographicCamera(Vector2ic viewport, float top, float bottom, float left, float right) {
        this(viewport, top, bottom, left, right, true);
    }

    public OrthographicCamera(Vector2ic viewportSize) {
        this(viewportSize,
                -.5f * viewportSize.y(), .5f * viewportSize.y(),
                -.5f * viewportSize.x(), .5f * viewportSize.x(),
                false
        );
    }

    private OrthographicCamera(
            Vector2ic viewport,
            float top,
            float bottom,
            float left,
            float right,
            boolean manualViewportSize
    ) {
        super(viewport);

        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;

        this.manualViewportSize = manualViewportSize;

        dirty();
        update();
    }

    @Override
    protected void updateViewportSize() {
        if (manualViewportSize) return;

        top = -.5f * viewportSize.y();
        bottom = .5f * viewportSize.y();
        left = -.5f * viewportSize.x();
        right = .5f * viewportSize.x();
    }

    @Override
    protected void updatePerspective() {
        //TODO proper zoom
        float zoom = 1f / this.zoom;
        perspective.setOrtho(zoom * left, zoom * right, zoom * bottom, zoom * top, zoom * near, zoom * far);
    }

}

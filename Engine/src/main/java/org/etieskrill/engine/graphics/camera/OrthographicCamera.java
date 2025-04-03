package org.etieskrill.engine.graphics.camera;

import org.joml.Vector2ic;

public class OrthographicCamera extends Camera {

    private float top, bottom, left, right;

    public OrthographicCamera(Vector2ic viewport, float top, float bottom, float left, float right) {
        super(viewport);

        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;

        dirty();
        update();
    }

    public OrthographicCamera(Vector2ic viewportSize) {
        this(viewportSize,
                -.5f * viewportSize.y(), .5f * viewportSize.y(),
                -.5f * viewportSize.x(), .5f * viewportSize.x()
        );
    }

    @Override
    protected void updateViewportSize() {
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

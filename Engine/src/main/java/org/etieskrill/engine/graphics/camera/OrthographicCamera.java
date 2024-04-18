package org.etieskrill.engine.graphics.camera;

import org.joml.*;

public class OrthographicCamera extends Camera {
    
    private float top, bottom, left, right;

    public OrthographicCamera(Vector2ic viewportSize, Vector3fc origin) {
        super(viewportSize);
        setPosition(origin);
        setPerspective(new Matrix4f().ortho(left, right, bottom, top, near, far));

        if (autoUpdate) update();
    }

    public OrthographicCamera(Vector2ic viewportSize) {
        this(viewportSize, new Vector3f(0f));
    }

    @Override
    protected void updatePerspective() {
        //TODO proper zoom
        float zoom = 1f / this.zoom;
        perspective.setOrtho(zoom * left, zoom * right, zoom * bottom, zoom * top, zoom * near, zoom * far);
    }

    @Override
    public Camera setPosition(Vector3fc position) {
        this.position.set(position);
        updateDimensions();
        if (autoUpdate) update();
        return this;
    }

    @Override
    public void setViewportSize(Vector2ic size) {
        this.viewportSize.set(size);
        updateDimensions();
        if (autoUpdate) update();
    }

    private void updateDimensions() {
        right = 0.5f * viewportSize.x();
        left = -right;
        bottom = 0.5f * viewportSize.y();
        top = -bottom;
    }
    
}

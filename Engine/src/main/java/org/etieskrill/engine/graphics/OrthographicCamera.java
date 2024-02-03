package org.etieskrill.engine.graphics;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class OrthographicCamera extends Camera {
    
    private final Vector2f size;
    private float top, bottom, left, right;
    
    public OrthographicCamera(Vector2f size, Vector3f origin) {
        super();
        this.size = new Vector2f();
        setSize(size);
        setPosition(origin);
        setPerspective(new Matrix4f().ortho(left, right, bottom, top, near, far));

        if (autoUpdate) update();
    }

    public OrthographicCamera(Vector2f size) {
        this(size, new Vector3f(0f));
    }

    @Override
    protected void updatePerspective() {
        //TODO proper zoom
        float zoom = 1f / this.zoom;
        perspective.setOrtho(zoom * left, zoom * right, zoom * bottom, zoom * top, zoom * near, zoom * far);
    }

    @Override
    public Camera setPosition(Vector3f position) {
        this.position.set(position);
        updateDimensions();
        if (autoUpdate) update();
        return this;
    }

    public void setSize(Vector2f size) {
        this.size.set(size);
        updateDimensions();
        if (autoUpdate) update();
    }

    private void updateDimensions() {
        right = 0.5f * size.x();
        left = -right;
        bottom = 0.5f * size.y();
        top = -bottom;
    }
    
}

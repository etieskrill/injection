package org.etieskrill.engine.graphics;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;
import glm_.vec3.Vec3;

import static glm_.Java.glm;

public class OrthographicCamera extends Camera {
    
    private final Vec2 size;
    private float top, bottom, left, right;
    
    public OrthographicCamera(Vec2 size, Vec3 origin) {
        super();
        this.size = new Vec2();
        setSize(size);
        setPosition(origin);
        setPerspective(glm.ortho(left, right, bottom, top, near, far));

        if (autoUpdate) update();
    }

    public OrthographicCamera(Vec2 size) {
        this(size, new Vec3(0f));
    }

    @Override
    protected void updatePerspective() {
        //TODO proper zoom
        float zoom = 1f / this.zoom;
        perspective.put(glm.ortho(zoom * left, zoom * right, zoom * bottom, zoom * top, zoom * near, zoom * far));
    }

    @Override
    public Camera setPosition(Vec3 position) {
        this.position.put(position);
        updateDimensions();
        if (autoUpdate) update();
        return this;
    }

    public void setSize(Vec2 size) {
        this.size.put(size);
        updateDimensions();
        if (autoUpdate) update();
    }

    private void updateDimensions() {
        bottom = position.getY();
        top = -size.getY() + bottom; //negative because ... reasons
        left = position.getX();
        right = size.getX() + left;
    }
    
}

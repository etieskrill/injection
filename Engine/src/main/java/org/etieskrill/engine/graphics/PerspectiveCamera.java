package org.etieskrill.engine.graphics;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;

import static glm_.Java.glm;

public class PerspectiveCamera extends Camera {

    private final Vec2 size;
    private float fov;

    public PerspectiveCamera(Vec2 size) {
        super();
        this.size = new Vec2(size);
        setFar(100f); //TODO figure out why this is negative in ortho
        setZoom(3.81f); //setFov(60f);
        setPerspective(glm.perspectiveFov((float) Math.toRadians(fov), size.getX(), size.getY(), near, far));

        if (autoUpdate) update();
    }

    @Override
    protected void updatePerspective() {
        this.fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
        this.perspective.put(glm.perspectiveFov((float) Math.toRadians(fov), size.getX(), size.getY(), near, far));
    }

    public Vec2 getSize() {
        return size;
    }

    public void setSize(Vec2 size) {
        this.size.put(size);
    }

    public float getFov() {
        return fov;
    }
    
    public void setFov(float fov) {
        this.fov = fov;
    }
    
}

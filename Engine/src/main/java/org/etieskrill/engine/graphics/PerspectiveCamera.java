package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.math.Vec3f;

public class PerspectiveCamera extends Camera {

    private final Vec2f size;
    private float fov;

    public PerspectiveCamera(Vec2f size) {
        super();
        this.size = new Vec2f(size);
        setFar(100f); //TODO figure out why this is negative in ortho
        setZoom(3.81f); //setFov(60f);
        setPerspective(new Mat4().perspectiveFov((float) Math.toRadians(fov), size.getX(), size.getY(), near, far));

        if (autoUpdate) update();
    }

    @Override
    protected void updatePerspective() {
        this.fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
        perspective.set(new Mat4().perspectiveFov((float) Math.toRadians(fov), size.getX(), size.getY(), near, far));
    }

    public Vec2f getSize() {
        return size;
    }

    public void setSize(Vec2f size) {
        this.size.set(size);
    }

    public float getFov() {
        return fov;
    }
    
    public void setFov(float fov) {
        this.fov = fov;
    }
    
}

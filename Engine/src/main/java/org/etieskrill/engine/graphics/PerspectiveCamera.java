package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import org.etieskrill.engine.math.Vec2f;

public class PerspectiveCamera extends Camera {

    private float fov;

    public PerspectiveCamera(Vec2f size) {
        super();
        setFar(100f); //TODO figure out why this is negative in ortho
        setFov(60f);
        setPerspective(new Mat4().perspectiveFov((float) Math.toRadians(fov), size.getX(), size.getY(), near, far));
    }

    @Override
    protected void updatePerspective() {
        this.fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
    }
    
    public float getFov() {
        return fov;
    }
    
    public void setFov(float fov) {
        this.fov = fov;
    }
    
}

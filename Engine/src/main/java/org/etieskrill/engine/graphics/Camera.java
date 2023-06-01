package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;

public abstract class Camera {
    
    protected final Vec3 position;
    protected float rotation;
    protected final Vec3 rotationAxis;
    protected float zoom;
    
    protected final Mat4 view, perspective, combined;
    
    protected float near, far;
    protected final Vec3 front, right, up, worldUp;
    
    protected Camera() {
        this.position = new Vec3();
        this.rotation = 0f;
        this.rotationAxis = new Vec3();
        this.zoom = 1f;
        this.view = new Mat4();
        this.perspective = new Mat4();
        this.combined = new Mat4();
        
        this.near = 0.1f;
        this.far = 100f;
        this.front = new Vec3();
        this.right = new Vec3();
        this.up = new Vec3();
        this.worldUp = new Vec3();
    }
    
    public void update() {
    }
    
    public Camera setPosition(Vec3 position) {
        this.position.set(position);
        return this;
    }
    
    public Camera setRotation(float rotation) {
        this.rotation = rotation;
        return this;
    }
    
    public Camera setRotationAxis(Vec3 rotationAxis) {
        this.rotationAxis.set(rotationAxis);
        return this;
    }
    
    public Camera setZoom(float zoom) {
        this.zoom = zoom;
        return this;
    }
    
    private void updateView() {
    }
    
    protected Camera setPerspective(Mat4 perspective) {
        this.perspective.set(perspective);
        updateCombined();
        return this;
    }
    
    private void updateCombined() {
        this.combined.set(view.mul_(perspective));
    }
    
    public Camera setNear(float near) {
        this.near = near;
        return this;
    }
    
    public Camera setFar(float far) {
        this.far = far;
        return this;
    }
    
    public Camera setWorldUp(Vec3 worldUp) {
        this.worldUp.set(worldUp);
        return this;
    }
    
}

package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;

public abstract class Camera {
    
    protected final Vec3 position;
    protected float rotation;
    protected final Vec3 rotationAxis;
    protected float scaleX, scaleY;
    protected float zoom;
    
    protected final Mat4 view, perspective, combined;
    
    protected float near, far;
    protected final Vec3 front, right, up, worldUp;

    protected float pitch, yaw, roll;
    
    protected Camera() {
        this.position = new Vec3();
        this.rotation = 0f;
        this.rotationAxis = new Vec3();
        this.scaleX = 1f;
        this.scaleY = 1f;
        this.zoom = 1f;
        this.view = new Mat4();
        this.perspective = new Mat4();
        this.combined = new Mat4();
        
        this.near = 0.1f;
        this.far = -100f;
        this.front = new Vec3();
        this.right = new Vec3();
        this.up = new Vec3();
        this.worldUp = new Vec3();
    }
    
    public void update() {
        updateView();
        updatePerspective();
        updateCombined();
    }

    /**
     * Moves the camera relative to its rotation.
     * @param translation vector to move by
     * @return itself for chaining
     */
    //TODO optimise
    public Camera translate(Vec3 translation) {
        Vec3 delta = new Vec3();
        delta.add(front.mul_(translation.x));
        delta.add(right.mul_(translation.y));
        delta.add(up.mul_(translation.z));
        return setPosition(this.position.add_(delta));
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
    
    public float getScaleX() {
        return scaleX;
    }
    
    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }
    
    public float getScaleY() {
        return scaleY;
    }
    
    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }
    
    public Camera setZoom(float zoom) {
        this.zoom = zoom;
        return this;
    }
    
    protected void updateView() {
        front.set(
                 Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                 Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
                .normalize();

        right.set(front.cross_(worldUp)).normalize();
        up.set(front.cross_(right)).normalize();

        Mat4 newMat = new Mat4()
                .lookAt(position, position.add_(front), up)
                .translate(new Vec3(position.x * 2, position.y * 2, position.z * 2));
                //.scale(0.0005f);
                //.scale(new Vec3(scaleX, scaleY, 1.0));
        this.view.set(newMat);
    }

    protected abstract void updatePerspective();
    
    protected Camera setPerspective(Mat4 perspective) {
        this.perspective.set(perspective);
        return this;
    }
    
    private void updateCombined() {
        this.combined.set(view.mul_(perspective));
    }
    
    public Mat4 getCombined() {
        return combined;
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

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setPitchBy(float pitch) {
        this.pitch += pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setYawBy(float yaw) {
        this.yaw += yaw;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public void setRollBy(float roll) {
        this.roll += roll;
    }

}

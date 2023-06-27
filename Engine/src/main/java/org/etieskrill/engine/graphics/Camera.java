package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;

import java.util.Arrays;

public abstract class Camera {
    
    protected final Vec3 position;
    protected float rotation;
    protected final Vec3 rotationAxis;
    protected float scaleX, scaleY;
    protected float zoom;
    
    protected final Mat4 view, perspective, combined;
    
    protected float near, far;
    protected final Vec3 front, right, up, worldUp;

    protected double pitch, yaw, roll;
    protected boolean clampPitch;
    
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
        this.worldUp = new Vec3(0f, 1f, 0f);
        
        this.clampPitch = true;
        
        update();
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
        add(delta, mul_(front, translation.z));
        add(delta, mul_(right, translation.x));
        add(delta, mul_(up, translation.y));
        add(this.position, delta);
        return this;
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
        
        Vec3 target = new Vec3(position.x + front.x, position.y + front.y, position.z + front.z);

        Mat4 newMat = new Mat4()
                .lookAt(position, target, up)
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
        this.combined.set(perspective.mul_(view));
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    @Deprecated
    public Mat4 getView() {
        return view;
    }
    
    @Deprecated
    public Mat4 getPerspective() {
        return perspective;
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

    public double getPitch() {
        return pitch;
    }
    
    public double getYaw() {
        return yaw;
    }
    
    public double getRoll() {
        return roll;
    }
    
    public Camera setOrientation(double pitch, double yaw, double roll) {
        if (clampPitch) {
            if (pitch > 89f) pitch = 89f;
            else if (pitch < -89f) pitch = -89f;
        }
        this.pitch = pitch;
        this.yaw = yaw;
        
        if (roll != 0f) throw new UnsupportedOperationException("plz dont use roll yet");
        this.roll = roll;
        
        return this;
    }

    public Camera orient(double pitch, double yaw, double roll) {
        this.pitch += pitch;
        if (clampPitch) {
            if (this.pitch > 89.0) this.pitch = 89.0;
            else if (this.pitch < -89.0) this.pitch = -89.0;
        }
        
        this.yaw += yaw;
        this.yaw %= 360.0;
        
        if (roll != 0.0) throw new UnsupportedOperationException("plz dont use roll yet");
        this.roll += roll;
        
        return this;
    }
    
    //TODO replace
    private static Vec3 add(Vec3 a, Vec3 b) {
        return a.set(a.x + b.x, a.y + b.y, a.z + b.z);
    }
    
    private static Vec3 mul_(Vec3 a, float s) {
        return new Vec3(a.x * s, a.y * s, a.z * s);
    }

}

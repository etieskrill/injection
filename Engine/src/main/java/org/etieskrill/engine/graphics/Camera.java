package org.etieskrill.engine.graphics;

import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;

import static glm_.Java.glm;

public abstract class Camera {
    
    protected final Vec3 position;
    protected float rotation;
    protected final Vec3 rotationAxis;
    protected float zoom;
    
    protected final Mat4 view, perspective, combined;
    
    protected float near, far;
    protected final Vec3 front, right, up, worldUp;

    protected double pitch, yaw, roll;
    protected boolean clampPitch;

    //TODO transform perform updates lazily instead of eagerly
    protected boolean autoUpdate;
    
    protected Camera() {
        this.position = new Vec3();
        this.rotation = 0f;
        this.rotationAxis = new Vec3();
        this.zoom = 1f;
        this.view = new Mat4();
        this.perspective = new Mat4();
        this.combined = new Mat4();

        //the near fucking clipping plane needs to be positive in order for the z-buffer to work, but only for perspective projection?
        this.near = 0.1f;
        this.far = -100f;
        this.front = new Vec3();
        this.right = new Vec3();
        this.up = new Vec3();
        this.worldUp = new Vec3(0f, 1f, 0f);
        
        this.clampPitch = true;
        this.autoUpdate = true;
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
        Vec3 delta = new Vec3()
                .plus(front.times(translation.getZ()))
                .plus(right.times(-translation.getX()))
                .plus(up.times(-translation.getY()));
        this.position.plusAssign(delta);
        if (autoUpdate) update();
        return this;
    }
    
    public Camera setPosition(Vec3 position) {
        this.position.put(position);
        if (autoUpdate) update();
        return this;
    }

    @Deprecated
    public Camera setRotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    @Deprecated
    public Camera setRotationAxis(Vec3 rotationAxis) {
        this.rotationAxis.put(rotationAxis);
        return this;
    }

    public float getZoom() {
        return zoom;
    }

    public Camera setZoom(float zoom) {
        this.zoom = zoom;
        if (autoUpdate) update();
        return this;
    }
    
    protected void updateView() {
        front.put(
                 Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                 Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalizeAssign();

        right.put(front.cross(worldUp).normalize());
        up.put(front.cross(right).normalize());
        
        Vec3 target = position.plus(front);
        
        this.view.put(glm
                .lookAt(position, target, up)
        );
//        System.out.println(position);
//        System.out.println(target);
//        System.out.println(view.get(3));
//        System.out.println();
    }

    protected abstract void updatePerspective();
    
    protected Camera setPerspective(Mat4 perspective) {
        this.perspective.put(perspective);
        if (autoUpdate) update();
        return this;
    }
    
    private void updateCombined() {
        this.combined.put(perspective.times(view));
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public Vec3 getDirection() {
        return new Vec3(
                Math.sin(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw)),
                Math.sin(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)),
                Math.cos(Math.toRadians(pitch)));
    }
    
    public Mat4 getCombined() {
        return combined;
    }
    
    public Camera setNear(float near) {
        this.near = near;
        if (autoUpdate) update();
        return this;
    }
    
    public Camera setFar(float far) { //TODO far is negative, 1.: find out why, 2.: adjust if necessary
        this.far = far;
        if (autoUpdate) update();
        return this;
    }
    
    public Camera setWorldUp(Vec3 worldUp) {
        this.worldUp.put(worldUp);
        if (autoUpdate) update();
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

        if (autoUpdate) update();
        return this;
    }

    public Camera orient(double pitch, double yaw, double roll) {
        this.pitch -= pitch;
        if (clampPitch) {
            if (this.pitch > 89.0) this.pitch = 89.0;
            else if (this.pitch < -89.0) this.pitch = -89.0;
        }
        
        this.yaw -= yaw;
        this.yaw %= 360.0;
        
        if (roll != 0.0) throw new UnsupportedOperationException("plz dont use roll yet");
        this.roll += roll;

        if (autoUpdate) update();
        return this;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

}

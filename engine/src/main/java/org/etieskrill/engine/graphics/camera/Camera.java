package org.etieskrill.engine.graphics.camera;

import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public abstract class Camera {

    protected final Vector3f position;
    protected float rotation;
    protected final Vector3f rotationAxis;
    protected float zoom;

    protected final Matrix4f view, perspective;
    protected Matrix4fc combined;
    
    protected float near, far;
    protected final Vector3f front, right, up, worldUp;

    protected double pitch, yaw, roll;
    protected boolean clampPitch;

    //TODO transform perform updates lazily instead of eagerly
    protected boolean autoUpdate;
    
    protected Camera() {
        this.position = new Vector3f();
        this.rotation = 0f;
        this.rotationAxis = new Vector3f();
        this.zoom = 1f;
        this.view = new Matrix4f();
        this.perspective = new Matrix4f();
        this.combined = new Matrix4f();

        //the near fucking clipping plane needs to be positive in order for the z-buffer to work, but only for perspective projection?
        this.near = 0.1f;
        this.far = -100f;
        this.front = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        this.worldUp = new Vector3f(0f, -1f, 0f);
        
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
    public Camera translate(Vector3fc translation) {
        Vector3f delta = relativeTranslation(translation);
        this.position.add(delta);
        if (autoUpdate) update();
        return this;
    }

    @Contract("_ -> new")
    public Vector3f relativeTranslation(Vector3fc translation) {
        return new Vector3f()
                .add(front.mul(translation.z()))
                .add(right.mul(-translation.x())) //TODO make positive
                .add(up.mul(-translation.y())); //TODO also make positive
    }

    public Vector3fc getPosition() {
        return position;
    }

    public Camera setPosition(Vector3fc position) {
        this.position.set(position);
        if (autoUpdate) update();
        return this;
    }

    @Deprecated
    public Camera setRotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    @Deprecated
    public Camera setRotationAxis(Vector3fc rotationAxis) {
        this.rotationAxis.set(rotationAxis);
        return this;
    }

    public float getZoom() {
        return zoom;
    }

    public Camera setZoom(float zoom) {
        this.zoom = Math.clamp(zoom, .01f, 10f);
        if (autoUpdate) update();
        return this;
    }
    
    protected void updateView() {
        front.set(
                 Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                 Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();

        right.set(front.cross(worldUp, new Vector3f()).normalize());
        up.set(front.cross(right, new Vector3f()).normalize());

        Vector3f target = new Vector3f(position).add(front);

        this.view.setLookAt(position, target, up);
    }

    protected abstract void updatePerspective();

    protected Camera setPerspective(Matrix4fc perspective) {
        this.perspective.set(perspective);
        if (autoUpdate) update();
        return this;
    }
    
    private void updateCombined() {
        this.combined = new Matrix4f(perspective).mul(view);
    }

    @Contract("-> new")
    public Vector3f getDirection() {
        return new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))))
            .normalize();
    }

    public Matrix4fc getCombined() {
        updateCombined();
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

    public Camera setWorldUp(Vector3fc worldUp) {
        this.worldUp.set(worldUp);
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

    @Override
    public String toString() {
        return "Camera{" +
                "position=" + position +
                ", rotation=" + rotation +
                ", rotationAxis=" + rotationAxis +
                ", zoom=" + zoom +
                ", near=" + near +
                ", far=" + far +
                ", front=" + front +
                ", right=" + right +
                ", up=" + up +
                ", worldUp=" + worldUp +
                ", pitch=" + pitch +
                ", yaw=" + yaw +
                ", roll=" + roll +
                '}';
    }
    
}
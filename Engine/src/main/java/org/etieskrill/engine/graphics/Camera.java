package org.etieskrill.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static glm_.Java.glm;

public abstract class Camera {

    protected final Vector3f position;
    protected float rotation;
    protected final Vector3f rotationAxis;
    protected float zoom;

    protected final Matrix4f view, perspective, combined;
    
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
    public Camera translate(Vector3f translation) {
        Vector3f delta = relativeTranslation(translation);
        this.position.plusAssign(delta);
        if (autoUpdate) update();
        return this;
    }

    public Vector3f relativeTranslation(Vector3f translation) {
        return new Vector3f()
                .plus(front.times(translation.getZ()))
                .plus(right.times(-translation.getX())) //TODO make positive
                .plus(up.times(-translation.getY())); //TODO also make positive
    }

    public Camera setPosition(Vector3f position) {
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
    public Camera setRotationAxis(Vector3f rotationAxis) {
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
        front.set(
                 Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                 Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();

        right.set(front.cross(worldUp).normalize());
        up.set(front.cross(right).normalize());

        Vector3f target = position.add(front);

        Matrix3f
        this.view.set(glm
                .lookAt(position, target, up)
        );
//        System.out.println(position);
//        System.out.println(target);
//        System.out.println(view.get(3));
//        System.out.println();
    }

    protected abstract void updatePerspective();

    protected Camera setPerspective(Matrix4f perspective) {
        this.perspective.set(perspective);
        if (autoUpdate) update();
        return this;
    }
    
    private void updateCombined() {
        this.combined.set(perspective.mul(view));
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getDirection() {
        return new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))))
            .normalize();
    }

    public Matrix4f getCombined() {
        return combined;
    }

    public Matrix4f getPerspective() {
        return perspective;
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

    public Camera setWorldUp(Vector3f worldUp) {
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

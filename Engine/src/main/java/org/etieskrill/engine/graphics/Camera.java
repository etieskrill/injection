package org.etieskrill.engine.graphics;

import glm_.mat3x3.Mat3;
import glm_.mat4x4.Mat4;
import glm_.quat.Quat;
import glm_.vec3.Vec3;

import static glm_.Java.glm;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public abstract class Camera {
    
    protected final Vec3 position;
    protected final Quat rotation;
    protected float zoom;
    
    protected final Mat4 view, perspective, combined;
    
    protected float near, far;
    protected final Vec3 worldUp;
    protected boolean clampPitch;
    protected boolean useWorldUp;

    //TODO transform perform updates lazily instead of eagerly
    protected boolean autoUpdate;
    
    protected Camera() {
        this.position = new Vec3();
        this.rotation = new Quat();
        this.zoom = 1f;
        this.view = new Mat4();
        this.perspective = new Mat4();
        this.combined = new Mat4();

        this.near = 0.1f;
        this.far = -100f;
        this.worldUp = new Vec3(0, 1, 0);
        
        this.clampPitch = true;
        this.useWorldUp = true;
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
    public Camera translate(Vec3 translation) {
        Vec3 delta = relativeTranslation(translation);
        this.position.plusAssign(delta);
        if (autoUpdate) update();
        return this;
    }
    
    public Vec3 relativeTranslation(Vec3 translation) {
        return rotation.toMat3().times(translation.times(1, 1, -1));
    }
    
    public Camera setPosition(Vec3 position) {
        this.position.put(position);
        if (autoUpdate) update();
        return this;
    }

    public Quat getRotation() {
        return rotation;
    }

    public Camera setRotation(float pitch, float yaw, float roll) {
        this.rotation.put(new Quat(new Vec3(toRadians(pitch), toRadians(yaw), toRadians(roll))));
        return this;
    }

    public Camera setRotation(Quat rotation) {
        this.rotation.put(rotation);
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
        Vec3 front = rotation.times(new Vec3(0, 0, -1)).normalize();
        Vec3 up = rotation.times(new Vec3(0, 1, 0)).normalize();
        Vec3 target = position.plus(front);

        this.view.put(glm
                .lookAt(position, target, useWorldUp ? worldUp : up)
        );
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
        return rotation.toMat3().times(new Vec3(0, 0, -1));
    }
    
    public Mat4 getCombined() {
        return combined;
    }
    
    public Mat4 getPerspective() {
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

    public Vec3 getWorldUp() {
        return worldUp;
    }

    public Camera setWorldUp(Vec3 worldUp) {
        this.worldUp.put(worldUp);
        if (autoUpdate) update();
        return this;
    }

    public Camera orient(double pitch, double yaw, double roll) {
        System.out.println(rotation.eulerAngles().getX() + " " + pitch);
        Quat qPitch = new Quat(new Vec3(toRadians(pitch), 0, 0)).normalize();
        Quat qYaw = new Quat(new Vec3(0, toRadians(yaw), 0)).normalize();
        Quat res = rotation.times(qYaw).times(qPitch).normalize();

        Quat qRoll = new Quat(new Vec3(0, 0, toRadians(roll))).normalize();
        worldUp.put(qRoll.times(worldUp).normalize());

        //TODO pitch clamping

        Vec3 front = res.times(new Vec3(0, 0, -1)).normalize();
        Vec3 right = worldUp.cross(front).normalize();
        Vec3 up = front.cross(right).normalize();

        Vec3 localUp = res.times(new Vec3(0, 1, 0)).normalize();
        Quat adjust = glm.rotation(localUp, up).normalize();
        Quat adjusted;
        if (Float.isNaN(adjust.x) || Float.isNaN(adjust.y) || Float.isNaN(adjust.z) || Float.isNaN(adjust.w))
            adjusted = res; //ugly workaround for the NaN component values on almost opposite vectors
        else
            adjusted = adjust.times(res).normalize();

        rotation.put(adjusted).normalizeAssign();

        if (autoUpdate) update();
        return this;
    }

    //TODO utils to be added:
    // - orbiting
    // - toggle to adjust global up if pitch caps
    // - transitions/animations

    //TODO fix euler angles, probably has something to do with the cross products at @l169
    public double getPitch() {
        double degrees = toDegrees(rotation.eulerAngles().getX());
        return degrees + (degrees < 0 ? degrees : 0);
    }

    public double getYaw() {
        double degrees = toDegrees(rotation.eulerAngles().getY());
        return degrees + (degrees < 0 ? degrees : 0);
    }

    public double getRoll() {
        double degrees = toDegrees(rotation.eulerAngles().getZ());
        return degrees + (degrees < 0 ? degrees : 0);
    }

    public Camera setClampPitch(boolean clampPitch) {
        this.clampPitch = clampPitch;
        return this;
    }

    public Camera setUseWorldUp(boolean useWorldUp) {
        this.useWorldUp = useWorldUp;
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
                ", zoom=" + zoom +
                ", near=" + near +
                ", far=" + far +
                ", worldUp=" + worldUp +
                '}';
    }
    
}

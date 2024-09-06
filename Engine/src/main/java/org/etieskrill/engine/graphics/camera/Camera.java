package org.etieskrill.engine.graphics.camera;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.jetbrains.annotations.Contract;
import org.joml.*;

import java.lang.Math;
import java.util.function.Consumer;

import static org.joml.Math.toRadians;

public abstract class Camera implements UniformMappable {

    protected final Vector3f position;
    protected final Quaternionf rotation;
    protected @Getter float zoom;

    protected final Vector2i viewportSize;

    protected final @Getter Matrix4f view, perspective;
    protected final Matrix4f combined;

    protected @Getter float near, far;
    protected final Vector3f worldUp;

    protected Vector3f eulerAngles;
    protected boolean clampPitch;

    protected @Accessors(chain = true)
    @Setter boolean orbit;
    protected @Accessors(chain = true)
    @Setter float orbitDistance;

    protected boolean dirty;

    protected Camera(Vector2ic viewportSize) {
        this.position = new Vector3f();
        this.rotation = new Quaternionf();
        this.zoom = 1f;

        this.viewportSize = new Vector2i(viewportSize);

        this.view = new Matrix4f();
        this.perspective = new Matrix4f();
        this.combined = new Matrix4f();

        //the near fucking clipping plane needs to be positive in order for the z-buffer to work, but only for perspective projection?
        this.near = 0.1f;
        this.far = -100f;
        this.worldUp = new Vector3f(0, 1, 0);

        this.eulerAngles = new Vector3f();
        this.clampPitch = true;

        this.orbit = false;
        this.orbitDistance = 1f;

        this.dirty = true;
        update();
    }

    public Vector3fc getPosition() {
        return position;
    }

    public Camera setPosition(Vector3fc position) {
        this.position.set(position);
        dirty();
        return this;
    }

    /**
     * Moves the camera relative to its rotation.
     *
     * @param translation vector to move by
     * @return itself for chaining
     */
    public Camera translate(Vector3fc translation) {
        position.add(relativeTranslation(translation));
        dirty();
        return this;
    }

    @Contract("_ -> new")
    public Vector3f relativeTranslation(Vector3fc translation) {
        return rotation.transform(translation, new Vector3f());
    }

    public Quaternionfc getRotation() {
        return rotation;
    }

    public Camera setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        dirty();
        return this;
    }

    public Camera setRotation(float pitch, float yaw, float roll) {
        if (clampPitch) {
            pitch %= 360;
            if (pitch > 89f) pitch = 89f;
            else if (pitch < -89f) pitch = -89f;
        }

        rotation.rotationYXZ(-toRadians(yaw), -toRadians(pitch), toRadians(roll)); //TODO should roll affect worldUp?

        dirty();
        return this;
    }

    public Camera rotate(Consumer<Quaternionf> rotation) {
        rotation.accept(this.rotation);
        dirty();
        return this;
    }

    public Camera rotate(float pitch, float yaw, float roll) {
        return setRotation(-(pitch + getPitch()), -(yaw + getYaw()), roll + getRoll());
    }

    public Vector2ic getViewportSize() {
        return viewportSize;
    }

    public void setViewportSize(Vector2ic viewportSize) {
        this.viewportSize.set(viewportSize);
    }

    public Camera setZoom(float zoom) {
        this.zoom = Math.clamp(zoom, .01f, 10f);
        dirty();
        return this;
    }

    protected void dirty() {
        dirty = true;
    }

    public void update() {
        if (!dirty) return;
        else dirty = false;

        rotation.getEulerAnglesYXZ(eulerAngles).mul(57.29577951308232f); //rad to deg constant

        updateView();
        updatePerspective();
        updateCombined();
    }

    protected void updateView() {
        Vector3f front = rotation.transform(new Vector3f(0, 0, 1)).normalize();
//        Vector3f up = rotation.transform(new Vector3f(0, 1, 0)).normalize();

        if (!orbit) {
            Vector3f target = new Vector3f(position).add(front);
            this.view.setLookAt(position, target, worldUp);
        } else {
            Vector3f orbitPosition = new Vector3f(position).sub(front.mul(orbitDistance));
            this.view.setLookAt(orbitPosition, position, worldUp);
        }
    }

    //TODO orbit cam using arcball

    protected abstract void updatePerspective();

    protected void setPerspective(Matrix4fc perspective) {
        this.perspective.set(perspective);
        dirty();
    }

    @Contract("-> new")
    public Vector3f getDirection() {
        return rotation.transform(new Vector3f(0, 0, 1)).normalize();
    }

    public Matrix4fc getCombined() {
        update();
        return combined;
    }

    private void updateCombined() {
        combined.set(perspective).mul(view);
    }

    public Camera setNear(float near) {
        this.near = near;
        dirty();
        return this;
    }

    public Camera setFar(float far) { //TODO far is negative, 1.: find out why, 2.: adjust if necessary
        this.far = far;
        dirty();
        return this;
    }

    public Camera setWorldUp(Vector3fc worldUp) {
        this.worldUp.set(worldUp);
        dirty();
        return this;
    }

    public Vector3f getEulerAngles() {
        update();
        return eulerAngles;
    }

    public float getPitch() {
        return getEulerAngles().x();
    }

    public float getYaw() {
        return getEulerAngles().y();
    }

    public float getRoll() {
        update();
        return getEulerAngles().z();
    }

    /**
     * Tests whether the world-space sphere at {@code center} with size {@code radius} intersects the volume projected
     * by the camera's view frustum in any point. Useful e.g. for simple frustum culling.
     *
     * @param center the center point of the sphere
     * @param radius the radius of the sphere
     * @return whether the sphere intersects the view frustum in any point
     */
    public abstract boolean frustumTestSphere(Vector3fc center, float radius);

    @Override
    public String toString() {
        return "Camera{" +
                ", position=" + position +
                ", rotation=" + rotation +
                ", rotationEulerAngles=" + eulerAngles +
                ", zoom=" + zoom +
                ", near=" + near +
                ", far=" + far +
                ", worldUp=" + worldUp +
                ", viewportSize=" + viewportSize +
                ", orbiting=" + orbit +
                ", orbitDistance=" + orbitDistance +
                '}';
    }

    @Override
    public boolean map(ShaderProgram.UniformMapper mapper) {
        update();
        mapper
                .map("view", getView())
                .map("perspective", getPerspective())
                .map("combined", getCombined())
                .map("position", position)
                .map("near", near)
                .map("far", far)
                .map("viewport", viewportSize)
                .map("aspect", viewportSize.x() / viewportSize.y());
        return true;
    }

}

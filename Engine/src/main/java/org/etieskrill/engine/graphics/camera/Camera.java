package org.etieskrill.engine.graphics.camera;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.jetbrains.annotations.Contract;
import org.joml.*;
import org.joml.primitives.AABBf;

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

    /**
     * Returns the final view position which the camera renders from. In non-orbit mode, this is identical to the
     * logical {@link Camera#position}.
     */
    private final @Getter Vector3f viewPosition = new Vector3f();

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

    /**
     * Returns the logical position of this {@code camera}, which is the view position in regular mode, and the orbit
     * centre in orbit mode.
     * <p>
     * Use the {@link #viewPosition} for rendering instead.
     *
     * @return the logical camera position
     */
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
        updateViewportSize();
        dirty();
    }

    protected abstract void updateViewportSize();

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
        Vector3f front = rotation.transform(viewPosition.set(0, 0, 1)).normalize();

        if (!orbit) {
            Vector3f target = front.add(position);
            this.view.setLookAt(position, target, worldUp);
        } else {
            front.mul(orbitDistance).negate().add(position);
            this.view.setLookAt(front, position, worldUp);
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
     * by the camera's view frustum in any point.
     *
     * @param center the center point of the sphere
     * @param radius the radius of the sphere
     * @return {@code true} if the sphere intersects the view frustum partially or fully, {@code false} otherwise
     */
    public boolean frustumTestSphere(Vector3fc center, float radius) {
        //TODO FrustumIntersection for more complicated stuff
//        FrustumIntersection
        return getCombined().testSphere(center.x(), center.y(), center.z(), radius);
    }

    /**
     * Tests whether the world-space axis-aligned bounding box {@code aabb} intersects the volume projected by the
     * camera's view frustum in any point.
     *
     * @param aabb the axis-aligned bounding box
     * @return {@code true} if the aabb intersects the view frustum partially or fully, {@code false} otherwise
     */
    public boolean frustumTestAABB(AABBf aabb) {
        return getCombined().testAab(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ());
    }

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
                .map("position", viewPosition)
                .map("near", near)
                .map("far", far)
                .map("viewport", viewportSize)
                .map("aspect", viewportSize.x() / viewportSize.y());
        return true;
    }

}

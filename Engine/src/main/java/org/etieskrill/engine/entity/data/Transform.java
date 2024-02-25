package org.etieskrill.engine.entity.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class Transform {

    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    private final Matrix4f transform;
    
    private boolean dirty = false;

    /**
     * Constructs a new identity {@link Transform}, such that the {@link #toMat()} call will result in an
     * {@link Matrix4f#identity() identity matrix}.
     */
    public Transform() {
        this(new Vector3f(), new Quaternionf(), new Vector3f(1));
    }

    /**
     * Constructs a new {@link Transform} based off of the given position, rotation, and scaling.
     *
     * @param position the 3d position vector
     * @param rotation the unit rotation quaternion
     * @param scale the 3d axis scaling vector
     */
    public Transform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;

        this.transform = new Matrix4f().identity();
        updateTransform();
    }

    /**
     * Constructs a copy of the specified {@link Transform} for convenience.
     *
     * @param transform the transform to be copied
     */
    public Transform(Transform transform) {
        this(new Vector3f(transform.position), new Quaternionf(transform.rotation), new Vector3f(transform.scale));
        this.dirty = transform.dirty;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Transform setPosition(@NotNull Vector3f vec) {
        this.position.set(requireNonNull(vec));
        dirty();
        return this;
    }
    
    public Transform setPosition(@NotNull Transform transform) {
        return setPosition(requireNonNull(transform).getPosition());
    }

    public Transform translate(@NotNull Vector3f vec) {
        this.position.add(requireNonNull(vec));
        dirty();
        return this;
    }
    
    public Transform translate(@NotNull Transform transform) {
        return translate(requireNonNull(transform).getPosition());
    }

    public Vector3f getScale() {
        return scale;
    }

    public Transform setScale(Vector3f scale) {
        if (scale.x() < 0 || scale.y() < 0 || scale.z() < 0)
            throw new IllegalArgumentException("Cannot apply negative scaling factor");
        this.scale.set(scale);
        dirty();
        return this;
    }
    
    public Transform setScale(float scale) {
        return setScale(new Vector3f(scale));
    }
    
    public Transform setScale(Transform transform) {
        return setScale(transform.getScale());
    }

    /**
     * A setter method which supplies the current scale using a consumer, enabling more complex calls by operating
     * directly on the scale <i>without</i> breaking the method chain like so:
     *
     * <pre>{@code
     * ...
     * .getTransform()
     * .translate(translation)
     * .applyScale(scl -> slc.mul(newScale))
     * .toMat()
     * }</pre>
     *
     * @param scale the scaling to be applied
     * @return the {@link Transform} for chaining
     */
    public Transform applyScale(Consumer<Vector3f> scale) {
        scale.accept(this.scale);
        return this;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Transform setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        dirty();
        return this;
    }

    public Transform setRotation(Transform transform) {
        return setRotation(transform.getRotation());
    }

    /**
     * A setter method which supplies the current rotation using a consumer, enabling more complex calls to the
     * {@link Quaternionf} by operating directly on it <i>without</i> breaking the method chain like so:
     *
     * <pre>{@code
     * ...
     * .getTransform()
     * .translate(translation)
     * .applyRotation(rot -> rot.rotationZ(angle))
     * .toMat()
     * }</pre>
     *
     * @param rotation the rotation to be applied
     * @return the {@link Transform} for chaining
     */
    public Transform applyRotation(Consumer<Quaternionf> rotation) {
        rotation.accept(this.rotation);
        return this;
    }

    @Contract("-> new")
    public Matrix4f toMat() {
        //Transform is lazily updated
        if (isDirty()) updateTransform();
        return new Matrix4f(transform);
    }

    public Transform set(Transform transform) {
        setPosition(transform);
        setRotation(transform);
        setScale(transform);
        return this;
    }

    public Transform set(Vector3f position, Quaternionf rotation, Vector3f scale) {
        setPosition(position);
        setRotation(rotation);
        setScale(scale);
        return this;
    }

    @Contract("_ -> new")
    public Transform apply(Transform transform) {
        return new Transform(
                new Vector3f(this.position).add(transform.position),
                new Quaternionf(this.rotation).mul(transform.rotation),
                new Vector3f(this.scale).mul(transform.scale));
    }
    
    void updateTransform() {
        this.transform.identity()
                .translate(position)
                .rotate(rotation)
                .scale(scale)
        ;
    }
    
    private void dirty() {
        dirty = true;
    }
    
    private boolean isDirty() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
    
    @Override
    public String toString() {
        return "Transform{" +
                "position=" + position +
                ", rotation=" + rotation +
                ", scale=" + scale +
                '}';
    }
    
}

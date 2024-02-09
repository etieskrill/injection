package org.etieskrill.engine.entity.data;

import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class Transform {

    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    private final Matrix4f transform;
    
    private boolean dirty = true;

    public Transform() {
        this(new Vector3f(), new Quaternionf(), new Vector3f(1));
    }

    public Transform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;

        this.transform = new Matrix4f().identity();
    }
    
    public Transform(Transform transform) {
        this(new Vector3f(transform.position), new Quaternionf(transform.rotation), new Vector3f(transform.scale));
        this.dirty = transform.dirty;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Transform setPosition(Vector3f vec) {
        this.position.set(vec);
        dirty();
        return this;
    }
    
    public Transform setPosition(Transform transform) {
        return setPosition(transform.getPosition());
    }

    public Transform translate(Vector3f vec) {
        this.position.add(vec);
        dirty();
        return this;
    }
    
    public Transform translate(Transform transform) {
        return translate(transform.getPosition());
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

    @Contract("_ -> new")
    public Transform apply(Transform transform) {
        return new Transform(
                new Vector3f(this.position).add(transform.position),
                new Quaternionf(this.rotation).mul(transform.rotation),
                new Vector3f(this.scale).mul(transform.scale));
    }
    
    private void updateTransform() {
        this.transform.identity()
                //TODO it's supposed to be Translation * Rotation * Scaling, and somehow, it just works a/w
                // i'm probs just too stupid to figure out why
                .translate(position)
                .rotate(rotation)
                .scale(scale);
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

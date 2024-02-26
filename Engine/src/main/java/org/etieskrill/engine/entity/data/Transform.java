package org.etieskrill.engine.entity.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.util.Objects;
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

    @Contract("_ -> new")
    public static Transform fromMatrix4f(Matrix4fc matrix) {
        Transform transform = new Transform(
                matrix.getTranslation(new Vector3f()),
                matrix.getUnnormalizedRotation(new Quaternionf()),
                matrix.getScale(new Vector3f())
        );
        if (Float.isNaN(transform.rotation.x) || Float.isNaN(transform.rotation.y)
                || Float.isNaN(transform.rotation.z) || Float.isNaN(transform.rotation.w)) {
            transform.applyRotation(Quaternionf::identity);
        }
        transform.transform.set(matrix);
        transform.dirty = false;
        return transform;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Transform setPosition(@NotNull Vector3fc vec) {
        this.position.set(requireNonNull(vec));
        dirty();
        return this;
    }
    
    public Transform setPosition(@NotNull Transform transform) {
        return setPosition(requireNonNull(transform).getPosition());
    }

    public Transform translate(@NotNull Vector3fc vec) {
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

    public Transform setScale(Vector3fc scale) {
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

    public Transform setRotation(Quaternionfc rotation) {
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

    public Transform set(Vector3fc position, Quaternionfc rotation, Vector3fc scale) {
        setPosition(position);
        setRotation(rotation);
        setScale(scale);
        return this;
    }

//    @Contract("_ -> param1")
//    public Transform apply(@NotNull Transform transform) {
//        return apply(requireNonNull(transform), this);
//    }
//
//    @Contract("_, _ -> param2")
//    public Transform apply(@NotNull Transform transform, @NotNull Transform target) {
//        requireNonNull(transform);
//        requireNonNull(target);
//
//        target.position.add(transform.position);
//        target.rotation.mul(transform.rotation);
//        target.scale.mul(transform.scale);
//
//        return target;
//    }

    @Contract(value = "_ -> this", mutates = "this")
    public Transform apply(@NotNull Transform transform) {
        requireNonNull(transform);

        this.position.add(transform.position);
        this.rotation.mul(transform.rotation);
        this.scale.mul(transform.scale);
        dirty();

        return this;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transform transform = (Transform) o;
        final float epsilon = 0.000001f; //kind of chosen arbitrarily, and probs not even portable across datastructures
        return position.equals(transform.position, epsilon)
                && rotation.equals(transform.rotation, epsilon)
                && scale.equals(transform.scale, epsilon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, rotation, scale, transform, dirty);
    }

}

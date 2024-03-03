package org.etieskrill.engine.entity.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class Transform implements TransformC {

    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    private final Matrix4f transform;
    
    private boolean dirty = false;

    /**
     * Constructs a new identity {@link Transform}, such that the {@link #getMatrix()} call will result in an
     * {@link Matrix4f#identity() identity matrix}.
     */
    public Transform() {
        this(new Vector3f(), new Quaternionf(), new Vector3f(1));
    }

    public Transform(Matrix4fc transform) {
        this(Transform.fromMatrix4f(transform));
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

        this.transform = new Matrix4f();
        updateTransform();
    }

    /**
     * Constructs a copy of the specified {@link Transform} for convenience.
     *
     * @param transform the transform to be copied
     */
    public Transform(TransformC transform) {
        this(new Vector3f(transform.getPosition()), new Quaternionf(transform.getRotation()), new Vector3f(transform.getScale()));
        this.dirty = transform.isDirty();
    }

    @Contract("_ -> new")
    public static Transform fromMatrix4f(Matrix4fc matrix) {
        Transform transform = new Transform();
        deconstructMatrixToTransform(matrix, transform);
        return transform;
    }

    private static void deconstructMatrixToTransform(Matrix4fc matrix, Transform target) {
        target.setPosition(matrix.getTranslation(target.getPosition()));
        target.setRotation(matrix.getUnnormalizedRotation(target.getRotation()));
        target.setScale(matrix.getScale(target.getScale()));

        if (!target.getRotation().isFinite())
            target.applyRotation(Quaternionf::identity);

        target.transform.set(matrix);
        target.dirty = false;
    }

    @Override
    public Vector3f getPosition() {
        return position;
    }

    public Transform setPosition(@NotNull Vector3fc vec) {
        this.position.set(requireNonNull(vec));
        dirty();
        return this;
    }
    
    public Transform setPosition(@NotNull TransformC transform) {
        return setPosition(requireNonNull(transform).getPosition());
    }

    public Transform translate(@NotNull Vector3fc vec) {
        this.position.add(requireNonNull(vec));
        dirty();
        return this;
    }
    
    public Transform translate(@NotNull TransformC transform) {
        return translate(requireNonNull(transform).getPosition());
    }

    @Override
    public Vector3f getScale() {
        return scale;
    }

    public Transform setScale(Vector3fc scale) {
        this.scale.set(scale);
        dirty();
        return this;
    }
    
    public Transform setScale(float scale) {
        return setScale(new Vector3f(scale));
    }
    
    public Transform setScale(TransformC transform) {
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
        dirty();
        return this;
    }

    @Override
    public Quaternionf getRotation() {
        return rotation;
    }

    public Transform setRotation(Quaternionfc rotation) {
        this.rotation.set(rotation);
        dirty();
        return this;
    }

    public Transform setRotation(TransformC transform) {
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
        dirty();
        return this;
    }

    @Override
    public Matrix4fc getMatrix() {
        //Transform is lazily updated
        if (shouldUpdate()) updateTransform();
        return transform;
    }

    public Transform set(TransformC transform) {
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

    @Override
    public Transform apply(@NotNull TransformC transform, @NotNull Transform target) {
        Matrix4f targetTransform = new Matrix4f(target.set(this).getMatrix());
        Matrix4fc _transform = transform.getMatrix();
        Matrix4fc newTransform = targetTransform.mul(_transform);
        deconstructMatrixToTransform(newTransform, target);

        return target;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public Transform apply(@NotNull TransformC transform) {
        return apply(transform, this);
    }

    @Override
    public Transform lerp(@NotNull TransformC other, float factor, @NotNull Transform target) {
        target.set(this);

        target.position.lerp(other.getPosition(), factor);
        target.rotation.slerp(other.getRotation(), factor);
        target.scale.lerp(other.getScale(), factor);
        target.dirty();

        return target;
    }

    @Contract(value = "_, _ -> this", mutates = "this")
    public Transform lerp(@NotNull TransformC other, float factor) {
        return lerp(other, factor, this);
    }

    public Transform identity() {
        this.position.set(0);
        this.rotation.identity();
        this.scale.set(1);
        this.transform.identity();

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

    private boolean shouldUpdate() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
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

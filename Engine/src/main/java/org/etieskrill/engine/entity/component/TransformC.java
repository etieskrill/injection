package org.etieskrill.engine.entity.component;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * An immutable view of a {@link Transform}. Prefer using this over the mutable version in order to preemptively avoid
 * reference issues.
 */
public interface TransformC {

    TransformC IDENTITY = new Transform();

    Vector3fc getPosition();
    Quaternionfc getRotation();
    Vector3fc getScale();

    /**
     * Gets the updated matrix of the transform.
     *
     * @return the transform matrix
     */
    @Contract("-> this")
    Matrix4fc getMatrix();

    @Contract("_, _ -> param2")
    Transform apply(@NotNull TransformC transform, @NotNull Transform target);

    //TODO find correct operation name
    @Contract("_, _ -> param2")
    Transform compose(@NotNull TransformC transform, @NotNull Transform target);

    @Contract("_, _, _ -> param3")
    Transform lerp(@NotNull TransformC other, float factor, @NotNull Transform target);

    boolean isIdentity();

    boolean isDirty();

}

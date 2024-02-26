package org.etieskrill.engine.entity.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * An immutable view of a {@link Transform}. Prefer using this over the mutable version in order to preemptively avoid
 * reference issues.
 */
public interface TransformC {

    Vector3fc getPosition();
    Quaternionfc getRotation();
    Vector3fc getScale();

    @Contract("-> this") //not technically correct, but there is no way to specify fields of the qualifying object
    Matrix4f toMat();

    @Contract("_, _ -> new")
    Transform apply(@NotNull TransformC transform, @NotNull Transform target);

    boolean isDirty();

}

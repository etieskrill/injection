package org.etieskrill.engine.entity.component

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.plus
import org.joml.timesAssign

/**
 * An immutable view of a [Transform]. Prefer using this over the mutable version.
 */
interface TransformC {

    companion object {
        val IDENTITY: TransformC = Transform()
    }

    val position: Vector3f
    val rotation: Quaternionf
    val scale: Vector3f

    val isIdentity: Boolean get() = this == IDENTITY

    /**
     * Gets the updated matrix of the transform.
     *
     * @return the transform matrix
     */
    val matrix: Matrix4fc

    infix fun apply(other: TransformC): Transform = applyInternal(other, Transform())
    operator fun times(other: TransformC) = apply(other)

    infix fun compose(other: TransformC): Transform = composeInternal(other, Transform())

    fun lerp(other: TransformC, t: Float): Transform = lerpInternal(other, t, Transform())

}

class Transform(
    position: Vector3f = Vector3f(),
    rotation: Quaternionf = Quaternionf(),
    scale: Vector3f = Vector3f(1f)
) : TransformC {

    override var position: Vector3f = position
        get() {
            isDirty = true //TODO observable primitives
            return field
        }
        set(value) {
            isDirty = true
            field.set(value)
        }

    override var rotation: Quaternionf = rotation
        get() {
            isDirty = true
            return field
        }
        set(value) {
            isDirty = true
            field.set(value)
        }

    override var scale: Vector3f = scale
        get() {
            isDirty = true
            return field
        }
        set(value) {
            isDirty = true
            field.set(value)
        }

    internal val internalMatrix = Matrix4f()
    override val matrix: Matrix4fc
        get() {
            updateTransform()
            return internalMatrix
        }

    internal var isDirty = true

    constructor(transform: Matrix4fc) : this(fromMatrix4f(transform))

    constructor(transform: TransformC) : this(
        Vector3f(transform.position),
        Quaternionf(transform.rotation),
        Vector3f(transform.scale)
    ) {
        isDirty = (transform as Transform).isDirty
    }

    /**
     * @return the local up unit vector
     */
    val up: Vector3f get() = rotation.transform(Vector3f(0f, 1f, 0f))

    fun set(transform: TransformC) {
        position = transform.position
        rotation = transform.rotation
        scale = transform.scale
    }

    override infix fun apply(other: TransformC): Transform = applyInternal(other, this)

    override infix fun compose(other: TransformC): Transform = composeInternal(other, this)

    override fun lerp(other: TransformC, t: Float): Transform = lerpInternal(other, t, this)

    fun identity(): Transform {
        this.position.set(0f)
        this.rotation.identity()
        this.scale.set(1f)
        this.internalMatrix.identity()

        return this
    }

    fun updateTransform() {
        if (!isDirty) return
        else isDirty = false

        this.internalMatrix.identity()
            .translate(position)
            .rotate(rotation)
            .scale(scale)
    }

    override fun toString(): String {
        return "Transform(position=$position, rotation=$rotation, scale=$scale)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transform) return false

        val epsilon = 0.000001f
        return position.equals(other.position, epsilon)
                && rotation.equals(other.rotation, epsilon)
                && scale.equals(other.scale, epsilon)
    }

    override fun hashCode(): Int {
        var hash = position.hashCode()
        hash = hash * 31 + rotation.hashCode()
        hash = hash * 31 + scale.hashCode()
        return hash
    }

}

internal fun fromMatrix4f(matrix: Matrix4fc, target: Transform = Transform()): Transform = target.apply {
    position = matrix.getTranslation(position)
    rotation = matrix.getUnnormalizedRotation(rotation).takeIf { it.isFinite } ?: Quaternionf()
    scale = matrix.getScale(scale)
    internalMatrix.set(matrix)
    isDirty = false
}

internal fun TransformC.applyInternal(other: TransformC, target: Transform): Transform {
    val targetMatrix = target.internalMatrix.set(matrix)
    targetMatrix *= other.matrix
    return fromMatrix4f(targetMatrix)
}

internal fun TransformC.composeInternal(other: TransformC, target: Transform): Transform {
    target.position.set(position)
    target.position += other.position
    target.rotation.set(rotation)
    target.rotation *= other.rotation
    target.scale.set(scale)
    target.scale *= other.scale
    return target
}

internal fun TransformC.lerpInternal(other: TransformC, t: Float, target: Transform): Transform {
    target.position.set(position)
    target.position.lerp(other.position, t)
    target.rotation.set(rotation)
    target.rotation.slerp(other.rotation, t)
    target.scale.set(scale)
    target.scale.lerp(other.scale, t)
    return target
}

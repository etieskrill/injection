package org.etieskrill.engine.entity.component

import net.bifreus.kml.Mat4
import net.bifreus.kml.Mat4c
import net.bifreus.kml.Quat
import net.bifreus.kml.Vec3

/**
 * An immutable view of a [Transform]. Prefer using this over the mutable version.
 */
interface TransformC {

    companion object {
        val IDENTITY: TransformC = Transform()
    }

    val position: Vec3
    val rotation: Quat
    val scale: Vec3

    val isIdentity: Boolean get() = this == IDENTITY

    /**
     * Gets the updated matrix of the transform.
     *
     * @return the transform matrix
     */
    val matrix: Mat4c

    infix fun apply(other: TransformC): Transform = applyInternal(other, Transform())
    operator fun times(other: TransformC) = apply(other)

    infix fun compose(other: TransformC): Transform = composeInternal(other, Transform())

    fun lerp(other: TransformC, t: Float): Transform = lerpInternal(other, t, Transform())

}

class Transform(
    position: Vec3 = Vec3(),
    rotation: Quat = Quat(),
    scale: Vec3 = Vec3(1f)
) : TransformC {

    override var position: Vec3 = position
        get() {
            isDirty = true //TODO observable primitives
            return field
        }
        set(value) {
            isDirty = true
            field(value)
        }

    override var rotation: Quat = rotation
        get() {
            isDirty = true
            return field
        }
        set(value) {
            isDirty = true
            field(value)
        }

    override var scale: Vec3 = scale
        get() {
            isDirty = true
            return field
        }
        set(value) {
            isDirty = true
            field(value)
        }

    internal val internalMatrix = Mat4()
    override val matrix: Mat4c
        get() {
            updateTransform()
            return internalMatrix
        }

    internal var isDirty = true

    constructor(transform: Mat4c) : this(fromMatrix4f(transform))

    constructor(transform: TransformC) : this(
        Vec3(transform.position),
        Quat(transform.rotation),
        Vec3(transform.scale)
    ) {
        isDirty = (transform as Transform).isDirty
    }

    /**
     * @return the local up unit vector
     */
    val up: Vec3 get() = rotation * Vec3.UP

    fun set(transform: TransformC) {
        position = transform.position
        rotation = transform.rotation
        scale = transform.scale
    }

    override infix fun apply(other: TransformC): Transform = applyInternal(other, this)

    override infix fun compose(other: TransformC): Transform = composeInternal(other, this)

    override fun lerp(other: TransformC, t: Float): Transform = lerpInternal(other, t, this)

    fun identity(): Transform {
        this.position(0)
        this.rotation()
        this.scale(1)
        this.internalMatrix()

        return this
    }

    fun updateTransform() {
        if (!isDirty) return
        else isDirty = false

        this.internalMatrix() translate position rotate rotation scale scale //what in seven hells have i wrought
    }

    override fun toString(): String {
        return "Transform(position=$position, rotation=$rotation, scale=$scale)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transform) return false

        return position == other.position
                && rotation == other.rotation
                && scale == other.scale
    }

    override fun hashCode(): Int {
        var hash = position.hashCode()
        hash = hash * 31 + rotation.hashCode()
        hash = hash * 31 + scale.hashCode()
        return hash
    }

}

internal fun fromMatrix4f(matrix: Mat4c, target: Transform = Transform()): Transform = target.apply {
    position = matrix.translation
    rotation = matrix.rotation.takeIf { it.isFinite } ?: Quat()
    scale = matrix.scaling
    internalMatrix(matrix)
    isDirty = false
}

internal fun TransformC.applyInternal(other: TransformC, target: Transform): Transform {
    var targetMatrix = target.internalMatrix(matrix)
    targetMatrix *= other.matrix
    return fromMatrix4f(targetMatrix, target)
}

internal fun TransformC.composeInternal(other: TransformC, target: Transform): Transform {
    target.position(position)
    target.position += other.position
    target.rotation(rotation)
    target.rotation *= other.rotation
    target.scale(scale)
    target.scale *= other.scale
    return target
}

internal fun TransformC.lerpInternal(other: TransformC, t: Float, target: Transform): Transform {
    target.position(position)
    target.position.lerp(other.position, t)
    target.rotation(rotation)
    target.rotation.slerp(other.rotation, t)
    target.scale(scale)
    target.scale.lerp(other.scale, t)
    return target
}

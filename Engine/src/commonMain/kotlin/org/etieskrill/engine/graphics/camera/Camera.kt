package org.etieskrill.engine.graphics.camera

import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper
import org.joml.FrustumRayBuilder
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.plusAssign
import org.joml.primitives.AABBfc
import org.joml.primitives.Rayf
import org.joml.timesAssign
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

abstract class Camera : UniformMappable {

    /**
     * Returns the logical position of this `camera`, which is the view position in regular mode, and the orbit
     * centre in orbit mode.
     *
     * Use the [viewPosition] for rendering instead.
     *
     * @return the logical camera position
     */
    var position: Vector3fc = Vector3f()
        set(value) {
            (field as Vector3f).set(value)
            dirty = true
        }
    var rotation: Quaternionfc = Quaternionf()
        set(value) {
            (field as Quaternionf).set(value)
            dirty = true
        }
    var zoom: Float = 1f
        set(value) {
            field = max(0.01f, min(value, 10f))
            dirty = true
        }

    var viewportSize: Vector2ic = Vector2i()
        set(value) {
            (field as Vector2i).set(value)
            updateViewportSize()
            dirty = true
        }

    val aspectRatio: Float get() = viewportSize.x().toFloat() / viewportSize.y()

    val direction: Vector3f get() = rotation.transform(Vector3f(0f, 0f, 1f)).normalize()

    val view: Matrix4fc = Matrix4f()
        get() {
            update()
            return field
        }
    var projection: Matrix4fc = Matrix4f()
        get() {
            update()
            return field
        }
        protected set(value) {
            (field as Matrix4f).set(value)
            dirty = true
        }
    val combined: Matrix4fc = Matrix4f()
        get() {
            update()
            return field
        }
    val invCombined: Matrix4fc = Matrix4f()
        get() {
            update()
            return field
        }

    var near: Float = 0.1f
        set(value) {
            field = value
            dirty = true
        }
    var far: Float = -100f
        set(value) {
            field = value
            dirty = true
        }
    var worldUp: Vector3fc = Vector3f(0f, 1f, 0f)
        set(value) {
            (field as Vector3f).set(value)
            dirty = true
        }

    val eulerAngles: Vector3fc = Vector3f()
        get() {
            update()
            return field
        }

    val pitch: Float get() = eulerAngles.x()
    val yaw: Float get() = eulerAngles.y()
    val roll: Float get() = eulerAngles.z()

    protected val clampPitch: Boolean = true

    var orbit: Boolean = false
        set(value) {
            field = value
            dirty = true
        }
    var orbitDistance: Float = 1f
        set(value) {
            field = value
            dirty = true
        }

    /**
     * Returns the final view position which the camera renders from. In non-orbit mode, this is identical to the
     * logical [position].
     */
    val viewPosition: Vector3fc = Vector3f()

    private val rayBuilder = FrustumRayBuilder()
    private val vector = Vector3f()

    protected var dirty: Boolean = true

    protected constructor(viewportSize: Vector2ic) {
        this.viewportSize = Vector2i(viewportSize)
        update()
    }

    /**
     * Moves the camera relative to its rotation.
     *
     * @param translation vector to move by
     */
    fun translate(translation: Vector3fc) {
        (position as Vector3f) += relativeTranslation(translation)
        dirty = true
    }

    fun relativeTranslation(translation: Vector3fc): Vector3f = rotation.transform(translation, Vector3f())

    fun setRotation(pitch: Float, yaw: Float, roll: Float) {
        var pitch = pitch
        if (clampPitch) {
            pitch %= 360
            if (pitch > 89f) pitch = 89f
            else if (pitch < -89f) pitch = -89f
        }

        (rotation as Quaternionf).rotationYXZ(
            -(yaw / (PI.toFloat() / 180f)),
            -(pitch / (PI.toFloat() / 180f)),
            roll / (PI.toFloat() / 180f) //TODO should roll affect worldUp?
        )

        dirty = true
    }

    fun rotate(rotation: Quaternionfc) {
        (this.rotation as Quaternionf) *= rotation
        dirty = true
    }

    fun rotate(pitch: Float, yaw: Float, roll: Float) =
        setRotation(-(this.pitch + pitch), -(this.yaw + yaw), this.roll + roll)

    protected abstract fun updateViewportSize()

    fun update() {
        if (!dirty) return
        else dirty = false

        rotation.getEulerAnglesYXZ(eulerAngles as Vector3f).mul(57.29577951308232f); //rad to deg constant

        updateView()
        updateProjection()
        updateCombined()
    }

    protected fun updateView() {
        val front = rotation.transform((viewPosition as Vector3f).set(0f, 0f, 1f)).normalize()

        if (!orbit) {
            val target = front.add(position)
            (this.view as Matrix4f).setLookAt(position, target, worldUp)
        } else {
            front.mul(orbitDistance).negate().add(position)
            (this.view as Matrix4f).setLookAt(front, position, worldUp)
        }
    }

    //TODO orbit cam using arcball

    protected abstract fun updateProjection()

    private fun updateCombined() {
        (combined as Matrix4f).set(projection).mul(view)
        (combined as Matrix4f).invert(invCombined as Matrix4f)
    }

    /**
     * Tests whether the world-space sphere at `center` with size `radius` intersects the volume projected
     * by the camera's view frustum in any point.
     *
     * @param center the center point of the sphere
     * @param radius the radius of the sphere
     * @return `true` if the sphere intersects the view frustum partially or fully, `false` otherwise
     */
    fun frustumTestSphere(center: Vector3fc, radius: Float): Boolean {
        //TODO FrustumIntersection for more complicated stuff
//        FrustumIntersection
        return combined.testSphere(center.x(), center.y(), center.z(), radius)
    }

    /**
     * Tests whether the world-space axis-aligned bounding box `aabb` intersects the volume projected by the
     * camera's view frustum in any point.
     *
     * @param aabb the axis-aligned bounding box
     * @return `true` if the aabb intersects the view frustum partially or fully, `false` otherwise
     */
    fun frustumTestAABB(aabb: AABBfc): Boolean =
        combined.testAab(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ())

    /**
     * Casts a ray in world space from this camera's [position] through the pixel defined by `x` and
     * `y` in the [viewport][viewportSize].
     *
     * `x` and `y` must range from 0 to [viewportSize] respectively.
     *
     * @param x horizontal viewport pixel coordinate
     * @param y vertical viewport pixel coordinate
     * @return world space ray with `origin` and `direction`
     */
    //TODO figure out if this works with ortho too (the docs kinda imply it doesn't)
    fun castViewportRay(x: Int, y: Int): Rayf {
        rayBuilder.set(combined)

        val ray = Rayf()

        rayBuilder.origin(vector)
        ray.oX = vector.x()
        ray.oY = vector.y()
        ray.oZ = vector.z()
        rayBuilder.dir(x.toFloat() / viewportSize.x(), 1f - (y.toFloat() / viewportSize.y()), vector)
        ray.dX = vector.x()
        ray.dY = vector.y()
        ray.dZ = vector.z()
        return ray
    }

    /**
     * Transfers a point in world coordinate space to this [Camera]'s normalized view space.
     *
     * The result is returned in right-handed coordinates, i.e. `+x` is **right**, `+y` is **up**,
     * and `+z` is **back**. Coordinates contained in the view frustum are in the range `[-1,1]`.
     *
     * @param point point in world space
     * @return point in view space
     */
    fun worldToView(point: Vector3fc): Vector3f {
        val homogeneousPoint = combined.transform(Vector4f(point, 1f))
        homogeneousPoint.div(homogeneousPoint.w)
        return homogeneousPoint.xyz(Vector3f())
    }

    override fun toString(): String {
        return "Camera{position=$position, rotation=$rotation, rotationEulerAngles=$eulerAngles, zoom=$zoom, near=$near, far=$far, worldUp=$worldUp, viewportSize=$viewportSize, orbiting=$orbit, orbitDistance=$orbitDistance}"
    }

    override fun map(mapper: UniformMapper): Boolean {
        update()
        mapper
                .map("view", view)
                .map("projection", projection)
                .map("combined", combined)
                .map("invCombined", invCombined)
                .map("position", viewPosition)
                .map("near", near)
                .map("far", far)
                .map("viewport", viewportSize)
                .map("aspect", aspectRatio)
        return true
    }

}

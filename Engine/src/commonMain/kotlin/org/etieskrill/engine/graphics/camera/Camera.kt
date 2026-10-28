package org.etieskrill.engine.graphics.camera

import net.bifreus.kml.AABBc
import net.bifreus.kml.Mat4
import net.bifreus.kml.Mat4c
import net.bifreus.kml.Quat
import net.bifreus.kml.Quatc
import net.bifreus.kml.Ray
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec4
import net.bifreus.kml.deg
import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper
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
    var position: Vec3c = Vec3()
        set(value) {
            (field as Vec3)(value)
            dirty = true
        }
    var rotation: Quatc = Quat()
        set(value) {
            (field as Quat)(value)
            dirty = true
        }
    var zoom: Float = 1f
        set(value) {
            field = max(0.01f, min(value, 10f))
            dirty = true
        }

    var viewportSize: Vec2ic = Vec2i()
        set(value) {
            (field as Vec2i)(value)
            updateViewportSize()
            dirty = true
        }

    val aspectRatio: Float get() = viewportSize.x.toFloat() / viewportSize.y

    val direction: Vec3 get() = (rotation * Vec3(0f, 0f, 1f)).normalize()

    val view: Mat4c = Mat4()
        get() {
            update()
            return field
        }
    var projection: Mat4c = Mat4()
        get() {
            update()
            return field
        }
        protected set(value) {
            (field as Mat4)(value)
            dirty = true
        }
    val combined: Mat4c = Mat4()
        get() {
            update()
            return field
        }
    val invCombined: Mat4c = Mat4()
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
    var worldUp: Vec3c = Vec3(0f, 1f, 0f)
        set(value) {
            (field as Vec3)(value)
            dirty = true
        }

    val eulerAngles: Vec3c = Vec3()
        get() {
            update()
            return field
        }

    val pitch: Float get() = eulerAngles.x
    val yaw: Float get() = eulerAngles.y
    val roll: Float get() = eulerAngles.z

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
    val viewPosition: Vec3c = Vec3()

    protected var dirty: Boolean = true

    protected constructor(viewportSize: Vec2ic) {
        this.viewportSize = Vec2i(viewportSize)
        update()
    }

    /**
     * Moves the camera relative to its rotation.
     *
     * @param translation vector to move by
     */
    fun translate(translation: Vec3c) {
        position += relativeTranslation(translation)
        dirty = true
    }

    fun relativeTranslation(translation: Vec3c): Vec3 = rotation * translation

    fun setRotation(pitch: Float, yaw: Float, roll: Float) {
        var pitch = pitch
        if (clampPitch) {
            pitch %= 360
            if (pitch > 89f) pitch = 89f
            else if (pitch < -89f) pitch = -89f
        }

        (rotation as Quat).eulerAnglesYXZ = Vec3(-pitch.deg, -yaw.deg, roll.deg)

        dirty = true
    }

    fun rotate(rotation: Quatc) {
        this.rotation *= rotation
        dirty = true
    }

    fun rotate(pitch: Float, yaw: Float, roll: Float) =
        setRotation(-(this.pitch + pitch), -(this.yaw + yaw), this.roll + roll)

    protected abstract fun updateViewportSize()

    fun update() {
        if (!dirty) return
        else dirty = false

        (eulerAngles as Vec3)((rotation.eulerAnglesYXZ * 57.29577951308232f)) //rad to deg constant

        updateView()
        updateProjection()
        updateCombined()
    }

    protected fun updateView() {
        val front = (rotation * (viewPosition as Vec3)(0f, 0f, 1f)).normalize()

        if (!orbit) {
            val target = front + position
            (this.view as Mat4).setLookAt(position, target, worldUp)
            viewPosition(position)
        } else {
            val eye = (front * -orbitDistance) + position
            (this.view as Mat4).setLookAt(eye, position, worldUp)
        }
    }

    //TODO orbit cam using arcball

    protected abstract fun updateProjection()

    private fun updateCombined() {
        (combined as Mat4)(projection * view)
        (invCombined as Mat4)(combined.inverted)
    }

    /**
     * Tests whether the world-space sphere at `center` with size `radius` intersects the volume projected
     * by the camera's view frustum in any point.
     *
     * @param center the center point of the sphere
     * @param radius the radius of the sphere
     * @return `true` if the sphere intersects the view frustum partially or fully, `false` otherwise
     */
    fun frustumTestSphere(center: Vec3c, radius: Float): Boolean = combined.frustumTestSphere(center, radius)

    /**
     * Tests whether the world-space axis-aligned bounding box `aabb` intersects the volume projected by the
     * camera's view frustum in any point.
     *
     * @param aabb the axis-aligned bounding box
     * @return `true` if the aabb intersects the view frustum partially or fully, `false` otherwise
     */
    fun frustumTestAABB(aabb: AABBc): Boolean = combined.frustumTestAABB(aabb)

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
    fun castViewportRay(x: Int, y: Int): Ray =
        Ray(combined, x.toFloat() / viewportSize.x, y.toFloat() / viewportSize.y)

    /**
     * Transfers a point in world coordinate space to this [Camera]'s normalized view space.
     *
     * The result is returned in right-handed coordinates, i.e. `+x` is **right**, `+y` is **up**,
     * and `+z` is **back**. Coordinates contained in the view frustum are in the range `[-1,1]`.
     *
     * @param point point in world space
     * @return point in view space
     */
    fun worldToView(point: Vec3c): Vec3 {
        var homogeneousPoint = combined * Vec4(point, 1f)
        homogeneousPoint /= homogeneousPoint.w
        return homogeneousPoint.xyz
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

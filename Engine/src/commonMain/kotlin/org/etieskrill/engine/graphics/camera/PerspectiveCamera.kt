package org.etieskrill.engine.graphics.camera

import net.bifreus.kml.Mat4
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.deg
import kotlin.math.PI

class PerspectiveCamera(
    viewportSize: Vec2ic
) : Camera(viewportSize) {

    /**
     * The (vertical) field of view in degrees.
     */
    var fov: Float = 60f; private set

    init {
        near = 0.1f //TODO figure out why this is negative in ortho
        far = 100f //TODO figure out why this is negative in ortho
        zoom = 3.81f // fov = 60f

        (projection as Mat4).setPerspective(fov / (PI.toFloat() / 180f), aspectRatio, near, far)

        dirty = true
        update()
    }

    override fun updateViewportSize() = Unit

    override fun updateProjection() {
        //TODO make fov and the whole zoom mechanic actually usable, cuz what in tarnation is this shit
        fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f)
        (projection as Mat4).setPerspective(fov.deg, aspectRatio, near, far)
    }

}

package org.etieskrill.engine.graphics.camera

import net.bifreus.kml.Mat4
import net.bifreus.kml.Vec2ic

class OrthographicCamera private constructor(
    viewportSize: Vec2ic,
    private var top: Float,
    private var bottom: Float,
    private var left: Float,
    private var right: Float,
    private val manualViewportSize: Boolean
) : Camera(viewportSize) {

    constructor(viewportSize: Vec2ic) : this(
        viewportSize,
        -.5f * viewportSize.y, .5f * viewportSize.y,
        -.5f * viewportSize.x, .5f * viewportSize.x,
        false
    )

    constructor(viewport: Vec2ic, top: Float, bottom: Float, left: Float, right: Float)
            : this(viewport, top, bottom, left, right, true)

    init {
        dirty = true
        update()
    }

    override fun updateViewportSize() {
        if (manualViewportSize) return

        top = -0.5f * viewportSize.y
        bottom = 0.5f * viewportSize.y
        left = -0.5f * viewportSize.x
        right = 0.5f * viewportSize.x
    }

    override fun updateProjection() {
        //TODO proper zoom
        val zoom = 1f / this.zoom
        (projection as Mat4).setOrtho(zoom * left, zoom * right, zoom * bottom, zoom * top, zoom * near, zoom * far)
    }

}

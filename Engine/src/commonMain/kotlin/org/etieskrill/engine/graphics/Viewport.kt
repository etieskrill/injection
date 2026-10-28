package org.etieskrill.engine.graphics

import net.bifreus.kml.Vec2
import org.etieskrill.engine.graphics.camera.Camera

//TODO fixed viewport for now
//technically, this does not matter much for now
class Viewport(
    private val size: Vec2,
    private val camera: Camera
)

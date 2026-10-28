package org.etieskrill.engine.entity.component

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.shader.Shader

class Drawable(
    val model: Model,
    var shader: Shader? = null,
    var isVisible: Boolean = true,
    var isWireframeEnabled: Boolean = false,
    var isOutlineEnabled: Boolean = false,
    var outlineWidth: Float = 0.05f,
    var textureScale: Vec2c? = Vec2(1f),
)

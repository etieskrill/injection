package org.etieskrill.engine.entity.component

import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Vector2f
import org.joml.Vector2fc

class Drawable(
    val model: Model,
    var shader: Shader? = null,
    var isVisible: Boolean = true,
    var isWireframeEnabled: Boolean = false,
    var isOutlineEnabled: Boolean = false,
    var outlineWidth: Float = 0.05f,
    var textureScale: Vector2fc? = Vector2f(1f),
)

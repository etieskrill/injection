package org.etieskrill.engine.graphics.gl.framebuffer

import org.joml.Vector2i
import org.joml.Vector2ic

@Suppress("DEPRECATION")
internal class ScreenBuffer(size: Vector2ic) : FrameBuffer(size, true) {
    override var size: Vector2ic =
        size //FIXME no clue if this property actually replaces the one in FrameBuffer - update, it does not, perhaps bcs fb is still java?
        set(value) {
            (field as Vector2i).set(value)
        }
}

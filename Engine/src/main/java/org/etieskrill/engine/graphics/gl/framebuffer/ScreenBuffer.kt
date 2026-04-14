package org.etieskrill.engine.graphics.gl.framebuffer

import org.joml.Vector2ic

@Suppress("DEPRECATION")
internal class ScreenBuffer(override val size: Vector2ic) : FrameBuffer(size, true)

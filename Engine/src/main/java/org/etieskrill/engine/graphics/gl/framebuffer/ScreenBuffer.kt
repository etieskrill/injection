package org.etieskrill.engine.graphics.gl.framebuffer

import org.joml.Vector2i
import org.joml.Vector2ic
import org.lwjgl.opengl.GL11C.*

internal class ScreenBuffer(size: Vector2ic) : FrameBuffer(size, mapOf(), 0) {
    override var size: Vector2ic =
        size //FIXME no clue if this property actually replaces the one in FrameBuffer - update, it does not, perhaps bcs fb is still java?
        set(value) {
            (field as Vector2i).set(value)
        }

    override fun init() {
        glBufferClearMask = GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT
        glColourDrawBuffers = intArrayOf(GL_BACK)
    }
}

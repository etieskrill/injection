package org.etieskrill.engine.graphics.gl.framebuffer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.FrameBufferInstance
import org.joml.Vector2i
import org.joml.Vector2ic
import org.lwjgl.opengl.GL11C.*

internal class ScreenBuffer(size: Vector2ic) : FrameBuffer(size, mapOf()) {
    override var size: Vector2ic =
        size //FIXME no clue if this property actually replaces the one in FrameBuffer - update, it does not, perhaps bcs fb is still java?
        set(value) {
            (field as Vector2i).set(value)
        }
}

internal class ScreenBufferInstance(
    descriptor: ScreenBuffer,
    context: GraphicsContext
) : FrameBufferInstance(descriptor, context) {
    override fun init() {
        id = 0
        glBufferClearMask = GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT
        glColourDrawBuffers = intArrayOf(GL_BACK)
    }

    override fun dispose() = error("Cannot dispose screen buffer")
}

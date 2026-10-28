package org.etieskrill.engine.graphics.framebuffer

import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import org.etieskrill.engine.graphics.GraphicsContext
import org.lwjgl.opengl.GL11C.*

internal class ScreenBuffer(size: Vec2ic) : FrameBuffer(size, mapOf()) {
    override var size: Vec2ic =
        size //FIXME no clue if this property actually replaces the one in FrameBuffer - update, it does not, perhaps bcs fb is still java?
        set(value) {
            (field as Vec2i)(value)
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

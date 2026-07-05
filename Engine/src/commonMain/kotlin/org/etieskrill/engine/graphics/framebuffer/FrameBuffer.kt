package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.GraphicsContextBound
import org.joml.Vector2ic
import org.joml.Vector4f
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer as DslFrameBuffer

expect class FrameBuffer : DslFrameBuffer, GraphicsContextBound, Disposable {

    var clearColour: Vector4f

    fun clear()

    companion object {
        fun getStandard(context: GraphicsContext, size: Vector2ic): FrameBuffer
        fun getColour(context: GraphicsContext, size: Vector2ic): FrameBuffer
    }

}

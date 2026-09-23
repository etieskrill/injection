package org.etieskrill.engine.graphics.pipeline

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer

class PostPassPipeline<S : PureShaderBuilder<*, *>>(
    shader: S,
    frameBuffer: FrameBuffer,
    opaque: Boolean = true,
    depthTest: Boolean = true,
    stencilMode: StencilMode = StencilMode.OFF
) : Pipeline<S>(
    4, PipelineConfig(
        alphaMode = if (opaque) AlphaMode.OPAQUE else AlphaMode.SOURCE_ALPHA,
        primitiveType = PrimitiveType.TRIANGLE_STRIP,
        cullingMode = CullingMode.NONE,
        depthTest = depthTest,
        writeDepth = false,
        stencilMode = stencilMode
    ), shader, frameBuffer
)

//TODO instancing
open class Pipeline<S : AbstractShader> private constructor(
    val vao: VertexArrayObject<*>?,
    /**
     * Only set when no vao is present.
     */
    val vertexCount: Int?,
    val config: PipelineConfig,
    val shader: S,
    /**
     * Passing `null` targets the window's default framebuffer.
     */
    val frameBuffer: FrameBuffer
) {
    constructor(vao: VertexArrayObject<*>, config: PipelineConfig, shader: S, frameBuffer: FrameBuffer)
            : this(vao, null, config, shader, frameBuffer)

    constructor(vertexCount: Int, config: PipelineConfig, shader: S, frameBuffer: FrameBuffer)
            : this(null, vertexCount, config, shader, frameBuffer)
}

data class PipelineConfig(
    val alphaMode: AlphaMode = AlphaMode.OPAQUE,

    val primitiveType: PrimitiveType = PrimitiveType.TRIANGLES,

    val cullingMode: CullingMode = CullingMode.BACK,

    val depthTest: Boolean = true,
    val writeDepth: Boolean = true,

    val fillMode: FillMode = FillMode.FILL,

    val pointSize: Float = 1f,

    val lineWidth: Float = 1f,
    val lineAntiAliasing: Boolean = true,

    val stencilMode: StencilMode = StencilMode.OFF
)

enum class AlphaMode { OPAQUE, SOURCE_ALPHA }

enum class PrimitiveType { POINTS, LINES, LINE_STRIP, TRIANGLES, TRIANGLE_STRIP }

enum class CullingMode { NONE, BACK, FRONT, FRONT_AND_BACK }

enum class FillMode { POINT, LINE, FILL }

enum class StencilMode {
    /**
     * Turns stencil operations off.
     */
    OFF,

    /**
     * Adds to the stencil buffer whatever is visible on screen after rendering the [Pipeline].
     *
     * That is what it should do.
     *
     * What it actually does: sets the stencil buffer wherever the rendered object is visible on screen that is not
     * occluded by an object in front of it (so far, so good), and ignores any objects rendered in front of this one
     * that have their stencil mode set to [OFF], meaning that objects rendered previously will occlude the stencil, and
     * those that come after will be happily ignored, which leads to inconsistent (and avoidable) behaviour.
     *
     * TODO conclusion: fix dis shit
     */
    SET_FRONT,

    /**
     * Masks what is rendered with the stencil buffer (what was rendered with [SET_FRONT]).
     */
    FILTER,

    /**
     * Negatively masks what is rendered with the stencil buffer (what was rendered with [SET_FRONT]).
     */
    FILTER_NOT,

    /**
     * Only activates writing to the stencil buffer. Primarily used so framebuffers can clear their stencil attachment.
     */
    CLEAR,

// TODO this option would _actually_ do nothing with the stencil buffer, but is only applicable if nothing else in
//  the frame uses the stencil buffer, otherwise non-stencil objects would not interact correctly with stencil stuff
//    IGNORE
}

package org.etieskrill.engine.graphics.pipeline

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import org.etieskrill.engine.graphics.gl.VertexArrayObject

class PostPassPipeline<S : PureShaderBuilder<*, *>>(
    shader: S,
    frameBuffer: FrameBuffer?,
    opaque: Boolean = true,
    depthTest: Boolean = true
) : Pipeline<S>(
    4, PipelineConfig(
        alphaMode = if (opaque) AlphaMode.OPAQUE else AlphaMode.SOURCE_ALPHA,
        primitiveType = PrimitiveType.TRIANGLE_STRIP,
        cullingMode = CullingMode.NONE,
        depthTest = depthTest
    ), shader, frameBuffer
)

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
    val frameBuffer: FrameBuffer?
) {
    constructor(vao: VertexArrayObject<*>, config: PipelineConfig, shader: S, frameBuffer: FrameBuffer?) : this(
        vao,
        null,
        config,
        shader,
        frameBuffer
    )

    constructor(vertexCount: Int, config: PipelineConfig, shader: S, frameBuffer: FrameBuffer?) : this(
        null,
        vertexCount,
        config,
        shader,
        frameBuffer
    )
}

data class PipelineConfig(
    val alphaMode: AlphaMode = AlphaMode.OPAQUE,

    val primitiveType: PrimitiveType = PrimitiveType.TRIANGLES,

    val cullingMode: CullingMode = CullingMode.BACK,

    val depthTest: Boolean = true,
    val writeDepth: Boolean = true,

    val drawMode: DrawMode = DrawMode.FILL,
    val frontFaceDrawMode: DrawMode = drawMode,
    val backFaceDrawMode: DrawMode = drawMode,

    val pointSize: Float = 1f,

    val lineWidth: Float = 1f,
    val lineAntiAliasing: Boolean = true,
)

enum class AlphaMode { OPAQUE, SOURCE_ALPHA }

enum class PrimitiveType { POINTS, LINES, LINE_STRIP, TRIANGLES, TRIANGLE_STRIP }

enum class CullingMode { NONE, BACK, FRONT, FRONT_AND_BACK }

enum class DrawMode { POINT, LINE, FILL }

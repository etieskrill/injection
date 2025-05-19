package org.etieskrill.engine.graphics.pipeline

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import org.etieskrill.engine.graphics.gl.VertexArrayObject

class PostPassPipeline<S : PureShaderBuilder<*, *>>(
    shader: S,
    frameBuffer: FrameBuffer?,
    opaque: Boolean = true
) : Pipeline<S>(
    4, PipelineConfig(
        if (opaque) AlphaMode.OPAQUE else AlphaMode.SOURCE_ALPHA,
        PrimitiveType.TRIANGLE_STRIP, CullingMode.NONE
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
)

enum class AlphaMode { OPAQUE, SOURCE_ALPHA }

enum class PrimitiveType { POINTS, TRIANGLES, TRIANGLE_STRIP }

enum class CullingMode { NONE, BACK, FRONT, FRONT_AND_BACK }

enum class DrawMode { POINT, LINE, FILL }

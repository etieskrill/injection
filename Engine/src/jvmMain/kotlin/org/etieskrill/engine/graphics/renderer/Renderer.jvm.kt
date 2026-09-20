package org.etieskrill.engine.graphics.renderer

import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.buffer.BufferType
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.pipeline.AlphaMode
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.FillMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.graphics.pipeline.StencilMode
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.util.FixedArrayDeque
import org.joml.Vector4f
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.opengl.GL45C.glCreateQueries
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

//TODO assure thread safety/passing
//TODO separate text renderer
actual class Renderer actual constructor(
    actual val context: GraphicsContext
) {

    companion object {
        private const val CLEAR_COLOUR = 0.25f//0.025f

        private val dummyVAO by lazy { glGenVertexArrays() }
    }

    private var _trianglesDrawn: Long = 0L
    actual val trianglesDrawn: Long get() = _trianglesDrawn

    private var _renderCalls: Long = 0L
    actual val renderCalls: Long get() = _renderCalls

    actual var isQueryGpuTime: Boolean = true
    private val timeQuery by lazy { glCreateQueries(GL_TIME_ELAPSED) }

    private var _averagedGpuTime: Duration = Duration.ZERO
    actual val averagedGpuTime: Duration get() = _averagedGpuTime

    private val gpuTimes = FixedArrayDeque<Duration>(100)

    private var _gpuDelay: Duration = Duration.ZERO
    actual val gpuDelay: Duration get() = _gpuDelay

    actual fun nextFrame() {
        context.checkThread()

        queryGpuTime()
        resetCounters()
    }

    actual fun render(pipeline: Pipeline<*>) = context.withContext {
        context.getFrameBuffer(pipeline.frameBuffer).bind()

        context.activeVertexArray?.unbind()
        //TODO validate that any textures/renderbuffers in active framebuffer are not bound in shader

        val isIndexed: Boolean
        if (pipeline.vao != null) {
            context.getVertexArray(pipeline.vao).bind()
            isIndexed = pipeline.vao.isIndexed
        } else {
            glBindVertexArray(dummyVAO)
            context.activeVertexArray = null
            isIndexed = false
        }

        val shader = when (pipeline.shader) {
            is Shader -> pipeline.shader
            is ShaderBuilder<*, *, *> -> pipeline.shader.shader as Shader
            else -> error("Unsupported shader type: ${pipeline.shader::class.simpleName}")
        }
        context.getShader(shader).bind()

        context.alphaMode = pipeline.config.alphaMode
        context.cullingMode = pipeline.config.cullingMode
        context.depthTest = pipeline.config.depthTest
        context.depthWrite = pipeline.config.writeDepth
        context.fillMode = pipeline.config.fillMode
        context.pointSize = pipeline.config.pointSize
        context.lineWidth = pipeline.config.lineWidth
        context.lineAntiAliasing = pipeline.config.lineAntiAliasing
        context.stencilMode = pipeline.config.stencilMode

        val vertexCount: Int
        if (isIndexed) {
            vertexCount = pipeline.vao!!.numElements
            glDrawElements(pipeline.config.primitiveType.gl, vertexCount, GL_UNSIGNED_INT, 0L)
        } else {
            vertexCount = when {
                pipeline.vao != null -> pipeline.vao.numElements
                pipeline.vertexCount != null -> pipeline.vertexCount
                else -> error("Pipeline without vertex array object must have vertex count set")
            }
            glDrawArrays(pipeline.config.primitiveType.gl, 0, vertexCount)
        }

        _renderCalls += 1
        _trianglesDrawn += when (pipeline.config.primitiveType) {
            PrimitiveType.POINTS, PrimitiveType.LINES, PrimitiveType.LINE_STRIP -> 0
            PrimitiveType.TRIANGLES -> vertexCount / 3
            PrimitiveType.TRIANGLE_STRIP -> vertexCount - 2
        }
    }

    internal actual fun queryGpuTime() {
        if (!isQueryGpuTime) {
            _averagedGpuTime = Duration.ZERO
            _gpuDelay = Duration.ZERO
            return
        }

        timeQuery.apply {
            glEndQuery(GL_TIME_ELAPSED)
            val time = TimeSource.Monotonic.markNow()
            val gpuTime = glGetQueryObjectui64(timeQuery, GL_QUERY_RESULT).nanoseconds
            _gpuDelay = TimeSource.Monotonic.markNow() - time
            gpuTimes.add(gpuTime)
            _averagedGpuTime = gpuTimes.map { it.inWholeNanoseconds }.average().nanoseconds
            glBeginQuery(GL_TIME_ELAPSED, timeQuery)
        }
    }

    actual fun resetCounters() {
        _trianglesDrawn = 0
        _renderCalls = 0
    }

}

private val PrimitiveType.gl get() = when (this) {
    PrimitiveType.POINTS -> GL_POINTS
    PrimitiveType.LINES -> GL_LINES
    PrimitiveType.LINE_STRIP -> GL_LINE_STRIP
    PrimitiveType.TRIANGLES -> GL_TRIANGLES
    PrimitiveType.TRIANGLE_STRIP -> GL_TRIANGLE_STRIP
}

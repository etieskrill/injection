package org.etieskrill.engine.graphics.renderer

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.pipeline.Pipeline
import kotlin.time.Duration

/**
 * A [Renderer] provides an abstracted way to render a [Pipeline] and its subclasses, such as
 * [PostPassPipeline][org.etieskrill.engine.graphics.pipeline.PostPassPipeline].
 *
 * It also collects various rendering statistics such as geometry count and render time.
 */
expect class Renderer {

    constructor(context: GraphicsContext)

    /**
     * Advances the renderer to draw the next frame to the screen buffer. Must only be called once at the beginning of
     * every frame.
     */
    fun nextFrame()

    /**
     * Renders the given [pipeline]. Any uniform values required for rendering must be set in the
     * [Pipeline.shader][org.etieskrill.engine.graphics.pipeline.Pipeline.shader] before this call.
     */
    fun render(pipeline: Pipeline<*>)

    /**
     * @return the graphics context this renderer is bound to
     */
    val context: GraphicsContext

    val trianglesDrawn: Long
    val renderCalls: Long

    var isQueryGpuTime: Boolean
    val averagedGpuTime: Duration
    val gpuDelay: Duration

    internal fun queryGpuTime()
    fun resetCounters()

}

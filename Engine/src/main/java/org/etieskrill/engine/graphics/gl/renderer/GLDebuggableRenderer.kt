package org.etieskrill.engine.graphics.gl.renderer

import org.etieskrill.engine.util.FixedArrayDeque
import org.etieskrill.engine.util.average
import org.lwjgl.opengl.GL15C.*
import org.lwjgl.opengl.GL33C.GL_TIME_ELAPSED
import org.lwjgl.opengl.GL33C.glGetQueryObjectui64
import org.lwjgl.opengl.GL45C.glCreateQueries

abstract class GLDebuggableRenderer(
    var queryGpuTime: Boolean = true,
    numGpuTimeSamples: Int = 100
) {

    var trianglesDrawn = 0; protected set
    var renderCalls = 0; protected set
    protected var lastTrianglesDrawn = 0
    protected var lastRenderCalls = 0
    protected var timeQuery = -1

    protected var gpuTime = 0L
    protected val gpuTimes = FixedArrayDeque<Long>(numGpuTimeSamples)

    var averagedGpuTime = 0L; protected set
    var gpuDelay = 0L; protected set

    protected fun queryGpuTime() {
        if (!queryGpuTime) {
            gpuTime = 0
            averagedGpuTime = 0
            gpuDelay = 0

            return
        }

        if (timeQuery == -1) {
            timeQuery = glCreateQueries(GL_TIME_ELAPSED)
        }
        glEndQuery(GL_TIME_ELAPSED)
        val time = System.nanoTime()
        gpuTime = glGetQueryObjectui64(timeQuery, GL_QUERY_RESULT)
        gpuDelay = System.nanoTime() - time
        gpuTimes.add(gpuTime)
        averagedGpuTime = gpuTimes.average().toLong()
        glBeginQuery(GL_TIME_ELAPSED, timeQuery)
    }

    fun resetCounters() {
        lastTrianglesDrawn = trianglesDrawn
        trianglesDrawn = 0
        lastRenderCalls = renderCalls
        renderCalls = 0
    }

}

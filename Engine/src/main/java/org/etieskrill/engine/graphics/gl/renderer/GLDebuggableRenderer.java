package org.etieskrill.engine.graphics.gl.renderer;

import lombok.Getter;
import org.etieskrill.engine.util.FixedArrayDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL33C.GL_TIME_ELAPSED;
import static org.lwjgl.opengl.GL33C.glGetQueryObjectui64;
import static org.lwjgl.opengl.GL45C.glCreateQueries;

public abstract class GLDebuggableRenderer {

    protected int trianglesDrawn, renderCalls;
    protected int lastTrianglesDrawn, lastRenderCalls;
    protected int timeQuery = -1;

    protected boolean queryGpuTime;
    protected @Getter long gpuTime;
    protected final FixedArrayDeque<Long> gpuTimes;
    protected @Getter long averagedGpuTime;
    protected @Getter long gpuDelay;

    private static final Logger logger = LoggerFactory.getLogger(GLDebuggableRenderer.class);

    public GLDebuggableRenderer() {
        this(true, 100);
    }

    public GLDebuggableRenderer(boolean queryGpuTime, int numGpuTimeSamples) {
        this.queryGpuTime = queryGpuTime;
        this.gpuTimes = new FixedArrayDeque<>(numGpuTimeSamples);
    }

    public int getTrianglesDrawn() {
        return lastTrianglesDrawn;
    }

    public int getRenderCalls() {
        return lastRenderCalls;
    }

    public boolean doesQueryGpuTime() {
        return queryGpuTime;
    }

    public void setQueryGpuTime(boolean queryGpuTime) {
        this.queryGpuTime = queryGpuTime;
        logger.info("Turned gpu time querying {}", queryGpuTime ? "on" : "off");
    }

    protected void queryGpuTime() {
        if (queryGpuTime) _queryGpuTime();
        else {
            gpuTime = 0;
            averagedGpuTime = 0;
            gpuDelay = 0;
        }
    }

    private void _queryGpuTime() {
        if (timeQuery == -1) {
            timeQuery = glCreateQueries(GL_TIME_ELAPSED);
        }

        glEndQuery(GL_TIME_ELAPSED);

        long time = System.nanoTime();
        gpuTime = glGetQueryObjectui64(timeQuery, GL_QUERY_RESULT);
        gpuDelay = System.nanoTime() - time;

        gpuTimes.push(gpuTime);
        averagedGpuTime = (long) gpuTimes.stream().mapToLong(value -> value).average().orElse(0d);

        glBeginQuery(GL_TIME_ELAPSED, timeQuery);
    }

    public void resetCounters() {
        lastTrianglesDrawn = trianglesDrawn;
        trianglesDrawn = 0;
        lastRenderCalls = renderCalls;
        renderCalls = 0;
    }

}

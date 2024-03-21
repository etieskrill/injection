package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.util.FixedArrayDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL33C.GL_TIME_ELAPSED;
import static org.lwjgl.opengl.GL33C.glGetQueryObjectui64;
import static org.lwjgl.opengl.GL45C.glCreateQueries;

public abstract class DebuggableRenderer {

    protected int trianglesDrawn, renderCalls;
    protected int lastTrianglesDrawn, lastRenderCalls;
    protected int timeQuery = -1;

    protected boolean queryGpuTime;
    protected long gpuTime;
    protected final FixedArrayDeque<Long> gpuTimes;
    protected long averagedGpuTime;
    protected long gpuDelay;

    private static final Logger logger = LoggerFactory.getLogger(DebuggableRenderer.class);

    public DebuggableRenderer() {
        this(true, 100);
    }

    public DebuggableRenderer(boolean queryGpuTime, int numGpuTimeSamples) {
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

    public long getGpuTime() {
        return gpuTime;
    }

    public long getAveragedGpuTime() {
        return averagedGpuTime;
    }

    public long getGpuDelay() {
        return gpuDelay;
    }

    protected void queryGpuTime() {
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

}

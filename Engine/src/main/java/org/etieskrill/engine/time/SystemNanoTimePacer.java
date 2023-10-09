package org.etieskrill.engine.time;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SystemNanoTimePacer implements LoopPacer {
    
    private static final long NANO_FACTOR = 1000000000, MILLI_FACTOR = 1000000;
    private static final int AVERAGE_FRAMERATE_SPAN = 20;
    
    private final Deque<Long> deltaBuffer = new ArrayDeque<>(AVERAGE_FRAMERATE_SPAN);
    private final int ignoredInitialReadings;
    
    private long targetDelta;
    private volatile long timeNow, timeLast, delta;
    private final AtomicLong timerTime = new AtomicLong();
    private volatile boolean timerPaused;
    private volatile double averageFPS;

    private volatile long totalFrames, localFrames;
    
    private boolean started = false;
    
    private long thread, lastThread = 0;
    
    public SystemNanoTimePacer(double targetDeltaSeconds, int ignoredInitialReadings) {
        this.targetDelta = (long) (targetDeltaSeconds * NANO_FACTOR);
        this.ignoredInitialReadings = ignoredInitialReadings;
    }
    
    public SystemNanoTimePacer(double targetDeltaSeconds) {
        this.targetDelta = (long) (targetDeltaSeconds * NANO_FACTOR);
        this.ignoredInitialReadings = (int) (10f / targetDelta);
    }
    
    @Override
    public void start() {
        if (started) throw new IllegalStateException("Pacer was already started");

        this.timeLast = System.nanoTime();
        this.deltaBuffer.addLast(0L);
        this.started = true;
    }
    
    @Override
    public void nextFrame() {
        if (!started) throw new IllegalStateException("Pacer must be started before call to nextFrame");
        if ((thread = Thread.currentThread().threadId()) != lastThread && lastThread != 0)
            throw new WrongThreadException("nextFrame must not be called from more than one thread");
        else lastThread = thread;
        
        updateTime();
        delta = timeNow - timeLast;
        if (!timerPaused) timerTime.addAndGet(delta);

        try {
            Thread.sleep((int) Math.max(targetDelta - delta, 0) / MILLI_FACTOR);
        } catch (InterruptedException e) {
            System.err.printf("[%s] Could not sleep", Thread.currentThread().getName());
        }

        if (totalFrames > ignoredInitialReadings) updateAverageFPS(System.nanoTime() - timeLast);
        timeLast = timeNow;

        incrementFrameCounters();
    }

    private void updateAverageFPS(long newDelta) {
        if (deltaBuffer.size() >= AVERAGE_FRAMERATE_SPAN) deltaBuffer.removeFirst();
        deltaBuffer.addLast(newDelta);

        long sum = 0;
        for (long value : deltaBuffer) {
            sum += value;
        }

        //TODO technically the value is wrong until the entire buffer is filled, but a buffer size call with every call seems pretty expensive in comparison
        averageFPS = (AVERAGE_FRAMERATE_SPAN * NANO_FACTOR) / (double)sum;
    }
    
    @Override
    public double getDeltaTimeSeconds() {
        return (double)delta / NANO_FACTOR;
    }
    
    @Override
    public double getSecondsElapsedTotal() {
        updateTime();
        return (double)timeNow / NANO_FACTOR;
    }

    @Override
    public void pauseTimer() {
        this.timerPaused = true;
    }

    @Override
    public void resumeTimer() {
        this.timerPaused = false;
    }

    @Override
    public void resetTimer() {
        this.timerTime.set(0L);
    }

    @Override
    public double getTime() {
        return (double)this.timerTime.get() / NANO_FACTOR;
    }

    @Override
    public double getAverageFPS() {
        return averageFPS;
    }

    @Override
    public long getTotalFramesElapsed() {
        return totalFrames;
    }

    @Override
    public long getFramesElapsed() {
        return localFrames;
    }

    @Override
    public void resetFrameCounter() {
        localFrames = 0;
    }

    @Override
    public double getTargetDeltaTime() {
        return (double)targetDelta / NANO_FACTOR;
    }
    
    @Override
    public synchronized void setTargetDeltaTime(double targetDeltaSeconds) {
        this.targetDelta = (long)(targetDeltaSeconds * NANO_FACTOR);
    }
    
    private void updateTime() {
        this.timeNow = System.nanoTime();
    }

    private synchronized void incrementFrameCounters() {
        totalFrames++;
        localFrames++;
    }
    
}

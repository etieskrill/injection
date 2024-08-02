package org.etieskrill.engine.time;

import org.etieskrill.engine.util.FixedArrayDeque;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;


public class SystemNanoTimePacer implements LoopPacer {

    private static final long NANO_FACTOR = 1_000_000_000, MILLI_FACTOR = 1_000_000;
    private static final int SPINLOCK_WINDOW_NANOS = 100_000;
    private static final int AVERAGE_FRAMERATE_SPAN = 20;

    private final FixedArrayDeque<Long> deltaBuffer = new FixedArrayDeque<>(AVERAGE_FRAMERATE_SPAN);

    private long targetDelta;
    private volatile long timeLast, frameTime, delta;
    private final AtomicLong timerTime = new AtomicLong();
    private volatile boolean timerPaused;
    private volatile double averageFPS;

    private volatile long totalFrames, localFrames;

    private boolean started = false;

    private long thread, lastThread = 0;

    public SystemNanoTimePacer(double targetDeltaSeconds) {
        this.targetDelta = (long) (targetDeltaSeconds * NANO_FACTOR);
    }

    @Override
    public void start() {
        if (started) throw new IllegalStateException("Pacer was already started");

        this.timeLast = getNanoTime();
        this.started = true;
    }

    @Override
    public void nextFrame() {
        if (!started) throw new IllegalStateException("Pacer must be started before call to nextFrame");
        if ((thread = Thread.currentThread().threadId()) != lastThread && lastThread != 0)
            throw new WrongThreadException("nextFrame must not be called from more than one thread");
        else lastThread = thread;

        frameTime = getNanoTime() - timeLast;

        long timeout = Math.max(targetDelta - frameTime - SPINLOCK_WINDOW_NANOS, 0);
        LockSupport.parkNanos(timeout);
        while ((getNanoTime() - timeLast) < targetDelta) {
        }

        long now = getNanoTime();
        delta = (now - timeLast);
        if (!timerPaused) timerTime.addAndGet(delta);

        updateAverageFPS(delta);
        timeLast = now;

        incrementFrameCounters();
    }

    private void updateAverageFPS(long newDelta) {
        deltaBuffer.push(newDelta);

        averageFPS = NANO_FACTOR / deltaBuffer.stream()
                .mapToLong(value -> value)
                .average()
                .orElse(0);
    }

    @Override
    public double getDeltaTimeSeconds() {
        return (double) delta / NANO_FACTOR;
    }

    @Override
    public double getSecondsElapsedTotal() {
        return (double) getNanoTime() / NANO_FACTOR;
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
    public boolean isPaused() {
        return this.timerPaused;
    }

    @Override
    public void resetTimer() {
        this.timerTime.set(0L);
    }

    @Override
    public double getTime() {
        return (double) this.timerTime.get() / NANO_FACTOR;
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
        return (double) targetDelta / NANO_FACTOR;
    }

    @Override
    public synchronized void setTargetDeltaTime(double targetDeltaSeconds) {
        this.targetDelta = (long) (targetDeltaSeconds * NANO_FACTOR);
    }

    private long getNanoTime() {
        return System.nanoTime();
    }

    private synchronized void incrementFrameCounters() {
        totalFrames++;
        localFrames++;
    }

}

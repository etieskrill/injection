package org.etieskrill.engine.time;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

public class SystemNanoTimePacer implements LoopPacer {
    
    private static final long NANO_FACTOR = 1000000000, MILLI_FACTOR = 1000000;
    private static final int AVERAGE_FRAMERATE_SPAN = 20;
    
    private final Deque<Long> deltaBuffer = new ArrayDeque<>(AVERAGE_FRAMERATE_SPAN);
    
    private long targetDelta;
    private volatile long timeNow, timeLast, delta;
    private volatile double averageFPS;
    
    private boolean started = false;
    
    private long thread, lastThread = 0;
    
    public SystemNanoTimePacer(double targetDeltaSeconds) {
        this.targetDelta = (long) (targetDeltaSeconds * NANO_FACTOR);
    }
    
    @Override
    public void start() {
        this.timeLast = System.nanoTime();
        this.deltaBuffer.addLast(0L);
        this.started = true;
    }
    
    @Override
    public void nextFrame() {
        if (!started) throw new IllegalStateException("Pacer must be started before call to nextFrame");
        if ((thread = Thread.currentThread().threadId()) != lastThread && lastThread != 0)
            throw new UnsupportedOperationException("nextFrame should not be called from more than one thread");
        else lastThread = thread;
        
        updateTime();
        delta = timeNow - timeLast;
        
        if (deltaBuffer.size() >= AVERAGE_FRAMERATE_SPAN) deltaBuffer.removeFirst();
        deltaBuffer.addLast(delta);
        
        long sum = 0;
        for (long value : deltaBuffer) {
            sum += value;
        }
        averageFPS = (AVERAGE_FRAMERATE_SPAN * NANO_FACTOR) / (double)sum;
    
        //System.out.println(averageFPS);
        
        timeLast = timeNow;
        
        try {
            Thread.sleep((int) Math.max(targetDelta - delta, 0) / MILLI_FACTOR);
        } catch (InterruptedException e) {
            System.err.printf("[%s] Could not sleep", Thread.currentThread().getName());
        }
    }
    
    @Override
    public double getSecondsSinceLastFrame() {
        return (double)delta / NANO_FACTOR;
    }
    
    @Override
    public double getSecondsElapsedTotal() {
        updateTime();
        return (double)timeNow / NANO_FACTOR;
    }
    
    @Override
    public double getAverageFPS() {
        return averageFPS;
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
    
}

package org.etieskrill.engine.time

import org.etieskrill.engine.util.FixedArrayDeque
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.max
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.TimeSource

class SystemNanoTimePacer(
    private val targetDelta: Duration
) : LoopPacer {

    companion object {
        val SPINLOCK_WINDOW = 100_000.nanoseconds
        val AVERAGE_FRAMERATE_SPAN_SECONDS = 2
    }

    private val deltaBuffer = FixedArrayDeque<Duration>(
        (AVERAGE_FRAMERATE_SPAN_SECONDS / targetDelta.toDouble(DurationUnit.SECONDS)).toInt()
    )

    private lateinit var timeStart: ComparableTimeMark
    private var timeLast = Duration.ZERO
    private var frameTime = Duration.ZERO
    private var delta = Duration.ZERO

    @OptIn(ExperimentalAtomicApi::class)
    private val timerTime = AtomicReference<Duration>(Duration.ZERO)
    private var timerPaused = false
    private var averageFPS = 0.0

    private var totalFrames = 0L
    private var localFrames = 0L

    private var isStarted = false

    override fun start() {
        if (isStarted) throw IllegalStateException("Pacer was already started")

        timeStart = TimeSource.Monotonic.markNow()
        isStarted = true
    }

    override fun nextFrame() {
        if (!isStarted) throw IllegalStateException("Pacer must be started before call to nextFrame")

        val now = timeStart.elapsedNow()
        frameTime = now - timeLast

        val timeout = max((targetDelta - frameTime - SPINLOCK_WINDOW).toLong(NANOSECONDS), 0).nanoseconds
        //TODO
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

    private fun getNanoTime() {
        return System.nanoTime();
    }

    private synchronized void incrementFrameCounters() {
        totalFrames++;
        localFrames++;
    }

}

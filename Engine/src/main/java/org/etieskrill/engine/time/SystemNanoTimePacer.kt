package org.etieskrill.engine.time

import org.etieskrill.engine.util.FixedArrayDeque
import java.util.concurrent.locks.LockSupport
import kotlin.math.max
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.TimeSource.Monotonic.markNow

class SystemNanoTimePacer(
    override var targetDeltaTime: Duration
) : LoopPacer {

    private companion object {
        val SPINLOCK_WINDOW = 100_000.nanoseconds
        const val AVERAGE_FRAMERATE_SPAN_SECONDS = 2
    }

    override var deltaTime = Duration.ZERO

    override val timeElapsedTotal: Duration get() = timeStart.elapsedNow()

    override var timerTime = Duration.ZERO; private set
    override var isTimerPaused = false

    override var averageFPS = 0.0; private set

    override var totalFramesElapsed = 0L; private set
    override var framesElapsed = 0L; private set

    private val deltaBuffer = FixedArrayDeque<Duration>(
        (AVERAGE_FRAMERATE_SPAN_SECONDS / targetDeltaTime.toDouble(DurationUnit.SECONDS)).toInt()
    )

    private lateinit var timeStart: ComparableTimeMark
    private lateinit var timeLast: ComparableTimeMark
    private var frameTime = Duration.ZERO

    private var isStarted = false

    override fun start() {
        if (isStarted) throw IllegalStateException("Pacer was already started")

        timeStart = markNow()
        timeLast = markNow()
        isStarted = true
    }

    override fun nextFrame() {
        if (!isStarted) throw IllegalStateException("Pacer must be started before call to nextFrame")

        frameTime = timeLast.elapsedNow()

        val timeout = max((targetDeltaTime - frameTime - SPINLOCK_WINDOW).toLong(NANOSECONDS), 0).nanoseconds
        //TODO probably create mpp blocking thingamajig
        LockSupport.parkNanos(timeout.inWholeNanoseconds)
        @Suppress("ControlFlowWithEmptyBody")
        while (timeLast.elapsedNow() < targetDeltaTime) {
        }

        deltaTime = timeLast.elapsedNow()
        if (!isTimerPaused) timerTime += deltaTime

        updateAverageFPS(deltaTime)
        timeLast = markNow()

        incrementFrameCounters()
    }

    private fun updateAverageFPS(newDelta: Duration) {
        deltaBuffer.add(newDelta)

        averageFPS = deltaBuffer
            .map { it.toDouble(DurationUnit.SECONDS) }
            .average()
    }

    override fun pauseTimer() {
        isTimerPaused = true
    }

    override fun resumeTimer() {
        isTimerPaused = false
    }

    override fun resetTimer() {
        timerTime = Duration.ZERO
    }

    override fun resetFrameCounter() {
        framesElapsed = 0
    }

    private fun getNanoTime(): Duration = System.nanoTime().nanoseconds

    private /*synchronized*/ fun incrementFrameCounters() {
        framesElapsed++
        totalFramesElapsed++
    }

}

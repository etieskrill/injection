package org.etieskrill.engine.time

import org.etieskrill.engine.util.FixedArrayDeque
import java.util.concurrent.locks.LockSupport
import kotlin.math.max
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.TimeSource.Monotonic.markNow

actual class LoopPacer actual constructor(
    actual var targetDeltaTime: Duration
) {

    @Deprecated("Only for interoperability with Java", level = DeprecationLevel.ERROR)
    constructor(deltaMillis: Int) : this(deltaMillis.milliseconds)

    private companion object {
        val SPINLOCK_WINDOW = 100_000.nanoseconds
        const val AVERAGE_FRAMERATE_SPAN_SECONDS = 2
    }

    private var _deltaTime = Duration.ZERO
    actual val deltaTime: Duration get() = _deltaTime

    actual val deltaTimeSeconds: Double get() = deltaTime.toDouble(SECONDS)

    actual val timeElapsedTotal: Duration get() = timeStart.elapsedNow()
    actual val timeElapsedTotalSeconds: Double get() = timeElapsedTotal.toDouble(SECONDS)

    private var _timerTime = Duration.ZERO
    actual val timerTime: Duration get() = _timerTime
    actual val timerTimeSeconds: Double get() = _timerTime.toDouble(SECONDS)
    actual var isTimerPaused = false

    private var _averageFPS = 0.0
    actual val averageFPS: Double get() = _averageFPS

    private var _totalFramesElapsed = 0L
    actual val totalFramesElapsed: Long get() = _totalFramesElapsed
    private var _framesElapsed = 0L
    actual val framesElapsed: Long get() = _framesElapsed

    private val deltaBuffer = FixedArrayDeque<Duration>(
        (AVERAGE_FRAMERATE_SPAN_SECONDS / targetDeltaTime.toDouble(SECONDS)).toInt()
    )

    private lateinit var timeStart: ComparableTimeMark
    private lateinit var timeLast: ComparableTimeMark
    private var frameTime = Duration.ZERO

    private var isStarted = false

    actual fun start() {
        if (isStarted) throw IllegalStateException("Pacer was already started")

        timeStart = markNow()
        timeLast = markNow()
        isStarted = true
    }

    actual fun nextFrame() {
        if (!isStarted) throw IllegalStateException("Pacer must be started before call to nextFrame")

        frameTime = timeLast.elapsedNow()

        val timeout = max((targetDeltaTime - frameTime - SPINLOCK_WINDOW).toLong(NANOSECONDS), 0).nanoseconds
        //TODO probably create mpp blocking thingamajig
        LockSupport.parkNanos(timeout.inWholeNanoseconds)
        @Suppress("ControlFlowWithEmptyBody")
        while (timeLast.elapsedNow() < targetDeltaTime) {
        }

        _deltaTime = timeLast.elapsedNow()
        if (!isTimerPaused) _timerTime += _deltaTime

        updateAverageFPS(_deltaTime)
        timeLast = markNow()

        incrementFrameCounters()
    }

    private fun updateAverageFPS(newDelta: Duration) {
        deltaBuffer.add(newDelta)

        _averageFPS = deltaBuffer
            .map { it.toDouble(DurationUnit.SECONDS) }
            .average()
    }

    actual fun pauseTimer() {
        isTimerPaused = true
    }

    actual fun resumeTimer() {
        isTimerPaused = false
    }

    actual fun resetTimer() {
        _timerTime = Duration.ZERO
    }

    actual fun resetFrameCounter() {
        _framesElapsed = 0
    }

    private /*synchronized*/ fun incrementFrameCounters() {
        _framesElapsed++
        _totalFramesElapsed++
    }

}

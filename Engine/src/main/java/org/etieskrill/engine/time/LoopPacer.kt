package org.etieskrill.engine.time

import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

//TODO rework interface and document
interface LoopPacer {

    fun start()

    /**
     * Primary function of a pacer. Must be called exactly once at any point in the target loop.
     */
    fun nextFrame()

    val deltaTime: Duration
    val deltaTimeSeconds: Double get() = deltaTime.toDouble(SECONDS)

    var targetDeltaTime: Duration

    fun pauseTimer()
    fun resumeTimer()
    var isTimerPaused: Boolean
    fun resetTimer()
    val timerTime: Duration
    val timerTimeSeconds: Double get() = timerTime.toDouble(SECONDS)

    val averageFPS: Double

    val totalFramesElapsed: Long
    val framesElapsed: Long
    fun resetFrameCounter()

    val timeElapsedTotal: Duration
    val timeElapsedTotalSeconds: Double get() = timeElapsedTotal.toDouble(SECONDS)

}

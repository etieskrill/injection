package org.etieskrill.engine.time

import kotlin.time.Duration

//TODO rework interface and document
expect class LoopPacer {

    constructor(targetDeltaTime: Duration)

    fun start()

    /**
     * Primary function of a pacer. Must be called exactly once at any point in the target loop.
     */
    fun nextFrame()

    val deltaTime: Duration
    val deltaTimeSeconds: Double

    var targetDeltaTime: Duration

    fun pauseTimer()
    fun resumeTimer()
    var isTimerPaused: Boolean
    fun resetTimer()
    val timerTime: Duration
    val timerTimeSeconds: Double

    val averageFPS: Double

    val totalFramesElapsed: Long
    val framesElapsed: Long
    fun resetFrameCounter()

    val timeElapsedTotal: Duration
    val timeElapsedTotalSeconds: Double

}

package org.etieskrill.engine.time

//TODO rework interface and document
interface LoopPacer {

    fun start()

    /**
     * Primary function of a pacer. Must be called exactly once at any point in the target loop.
     */
    fun nextFrame()

    fun getDeltaTimeSeconds(): Double
    fun getSecondsElapsedTotal(): Double

    fun pauseTimer()
    fun resumeTimer()
    fun isPaused(): Boolean
    fun resetTimer()
    fun getTime(): Double

    fun getAverageFPS(): Double

    fun getTotalFramesElapsed(): Long
    fun getFramesElapsed(): Long
    fun resetFrameCounter()

    fun getTargetDeltaTime(): Double
    fun setTargetDeltaTime(deltaSeconds: Double)

}

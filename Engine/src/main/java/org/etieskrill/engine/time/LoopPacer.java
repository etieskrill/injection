package org.etieskrill.engine.time;

//TODO rework interface and document
public interface LoopPacer {
    
    void start();
    
    /**
     * Primary function of a pacer. Must be called exactly once at any point in the target loop.
     */
    void nextFrame();
    
    double getDeltaTimeSeconds();
    double getSecondsElapsedTotal();

    void pauseTimer();
    void resumeTimer();
    boolean isPaused();
    void resetTimer();
    double getTime();

    double getAverageFPS();

    long getTotalFramesElapsed();
    long getFramesElapsed();
    void resetFrameCounter();

    double getTargetDeltaTime();
    void setTargetDeltaTime(double deltaSeconds);
    
}

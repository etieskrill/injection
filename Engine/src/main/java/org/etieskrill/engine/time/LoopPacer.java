package org.etieskrill.engine.time;

public interface LoopPacer {
    
    void start();
    
    /**
     * Primary function of a pacer. Must be called exactly once at any point in the target loop.
     */
    void nextFrame();
    
    double getDeltaTimeSeconds();
    double getSecondsElapsedTotal();
    
    double getAverageFPS();

    long getTotalFramesElapsed();
    long getFramesElapsed();
    void resetFrameCounter();

    double getTargetDeltaTime();
    void setTargetDeltaTime(double deltaSeconds);
    
}

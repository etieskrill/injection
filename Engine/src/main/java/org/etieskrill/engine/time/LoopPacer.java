package org.etieskrill.engine.time;

public interface LoopPacer {
    
    void start();
    
    /**
     * Blocking
     */
    void nextFrame();
    
    double getSecondsSinceLastFrame();
    double getSecondsElapsedTotal();
    
    double getAverageFPS();
    
    double getTargetDeltaTime();
    void setTargetDeltaTime(double deltaSeconds);
    
}

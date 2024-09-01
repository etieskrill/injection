package org.etieskrill.engine.time;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class StepTimer {

    private long time = 0;
    private final Logger targetLogger;

    public StepTimer() {
        this(logger);
    }

    public StepTimer(Logger logger) {
        this.targetLogger = logger;
    }

    public void start() {
        time = System.nanoTime();
    }

    public void log(String subject) {
        long now = System.nanoTime();
        targetLogger.debug("{}: {}s", subject, (now - time) / 1_000_000_000d);
        time = now;
    }

    public void logDetail(String subject) {
        long now = System.nanoTime();
        targetLogger.trace("{}: {}s", subject, (now - time) / 1_000_000_000d);
        time = now;
    }

}

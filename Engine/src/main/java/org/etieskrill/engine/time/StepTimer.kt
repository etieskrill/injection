package org.etieskrill.engine.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepTimer {

    private long time = 0;
    private final Logger targetLogger;

    private static final Logger logger = LoggerFactory.getLogger(StepTimer.class);

    public StepTimer() {
        this(logger);
    }

    public StepTimer(Logger logger) {
        this.targetLogger = logger;
    }

    public void start() {
        time = System.nanoTime();
    }

    public void log(String message) {
        debug(message);
    }

    public void debug(String message) {
        targetLogger.debug("{} [{}s]", message, getTimeFormatted());
    }

    public void info(String subject) {
        targetLogger.info("{} [{}s]", subject, getTimeFormatted());
    }

    public void trace(String subject) {
        targetLogger.trace("{} [{}s]", subject, getTimeFormatted());
    }

    private double getTime() {
        long now = System.nanoTime();
        double delta = (now - time) / 1_000_000_000d;
        time = now;
        return delta;
    }

    private String getTimeFormatted() {
        return String.format("%.3f", getTime());
    }

}

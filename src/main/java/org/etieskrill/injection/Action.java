package org.etieskrill.injection;

/**
 * A shadow of {@link java.lang.Runnable} in case own functionality is needed sometime.
 */
@FunctionalInterface
public interface Action extends Runnable {
}

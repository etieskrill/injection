package org.etieskrill.engine.input;

/**
 * Accepts generic input events from any input method which can provide either {@code PRESS} or {@code RELEASE}.
 */
@FunctionalInterface
public interface KeyInputHandler {

    boolean invoke(Key.Type type, int key, int action, int modifiers);
    
}

package org.etieskrill.engine.input;

import org.jetbrains.annotations.NotNull;

/**
 * Accepts generic input events from any input method which can provide either {@code PRESS} or {@code RELEASE}.
 */
@FunctionalInterface
public interface KeyInputHandler {

    //TODO type is probably useless, remove, and use enum types via lookups
    boolean invoke(@NotNull Key.Type type, int key, int action, int modifiers);

    default boolean invokeCharacter(char character) {
        return false;
    }

}

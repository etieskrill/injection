package org.etieskrill.engine.input;

import java.util.Arrays;
import java.util.Objects;

public class InputBinds {
    
    public static InputManager of(InputBinding... bindings) {
        if (Arrays.stream(bindings).anyMatch(Objects::isNull))
            throw new NullPointerException("Invalid list of inputs");
        InputManager manager = new InputManager();
        for (InputBinding binding : bindings) {
            manager.addBinding(binding.getInput(), binding.getTrigger(), binding.getAction());
        }
        return manager;
    }
    
}

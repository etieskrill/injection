package org.etieskrill.engine.input;

import java.util.Objects;

public class InputBinding {
    
    private final KeyInput keyInput;
    private final Trigger trigger;
    private final Action action;
    
    public enum Trigger {
        ON_PRESS,
        PRESSED,
        ON_TOGGLE,
        TOGGLED
    }
    
    public InputBinding(KeyInput keyInput, SimpleAction action) {
        this(keyInput, Trigger.ON_PRESS, action);
    }
    
    public InputBinding(KeyInput keyInput, DeltaAction action) {
        this(keyInput, Trigger.ON_PRESS, action);
    }
    
    public InputBinding(KeyInput keyInput, Trigger trigger, SimpleAction action) {
        this.keyInput = Objects.requireNonNull(keyInput);
        this.trigger = trigger;
        this.action = Objects.requireNonNull(action);
    }
    
    public InputBinding(KeyInput keyInput, Trigger trigger, DeltaAction action) {
        this.keyInput = Objects.requireNonNull(keyInput);
        this.trigger = trigger;
        this.action = Objects.requireNonNull(action);
    }
    
    public KeyInput getInput() {
        return keyInput;
    }
    
    public Trigger getTrigger() {
        return trigger;
    }
    
    public Action getAction() {
        return action;
    }
    
}

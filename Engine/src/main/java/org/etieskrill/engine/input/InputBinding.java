package org.etieskrill.engine.input;

import java.util.Objects;

public class InputBinding {
    
    private final Input input;
    private final Trigger trigger;
    private final Action action;
    
    public enum Trigger {
        ON_PRESS,
        PRESSED,
        ON_TOGGLE,
        TOGGLED
    }
    
    public InputBinding(Input input, SimpleAction action) {
        this(input, Trigger.ON_PRESS, action);
    }
    
    public InputBinding(Input input, DeltaAction action) {
        this(input, Trigger.ON_PRESS, action);
    }
    
    public InputBinding(Input input, Trigger trigger, SimpleAction action) {
        this.input = Objects.requireNonNull(input);
        this.trigger = trigger;
        this.action = Objects.requireNonNull(action);
    }
    
    public InputBinding(Input input, Trigger trigger, DeltaAction action) {
        this.input = Objects.requireNonNull(input);
        this.trigger = trigger;
        this.action = Objects.requireNonNull(action);
    }
    
    public Input getInput() {
        return input;
    }
    
    public Trigger getTrigger() {
        return trigger;
    }
    
    public Action getAction() {
        return action;
    }
    
}

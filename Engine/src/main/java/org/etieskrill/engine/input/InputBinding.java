package org.etieskrill.engine.input;

import org.etieskrill.engine.input.action.Action;
import org.etieskrill.engine.input.action.DeltaAction;
import org.etieskrill.engine.input.action.SimpleAction;

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
    
    public InputBinding(KeyInput.Keys input, SimpleAction action) {
        this(input.getInput(), Trigger.ON_PRESS, action);
    }
    
    public InputBinding(KeyInput input, SimpleAction action) {
        this(input, Trigger.ON_PRESS, action);
    }
    
    public InputBinding(KeyInput.Keys input, Trigger trigger, SimpleAction action) {
        this.keyInput = Objects.requireNonNull(input).getInput();
        this.trigger = Objects.requireNonNull(trigger);
        this.action = Objects.requireNonNull(action);
    }
    
    public InputBinding(KeyInput input, Trigger trigger, SimpleAction action) {
        this.keyInput = Objects.requireNonNull(input);
        this.trigger = Objects.requireNonNull(trigger);
        this.action = Objects.requireNonNull(action);
    }
    
    public InputBinding(KeyInput.Keys input, DeltaAction action) {
        this(input.getInput(), Trigger.ON_PRESS, action);
    }
    
    public InputBinding(KeyInput input, DeltaAction action) {
        this(input, Trigger.ON_PRESS, action);
    }
    
    public InputBinding(KeyInput.Keys input, Trigger trigger, DeltaAction action) {
        this.keyInput = Objects.requireNonNull(input).getInput();
        this.trigger = Objects.requireNonNull(trigger);
        this.action = Objects.requireNonNull(action);
    }
    
    public InputBinding(KeyInput input, Trigger trigger, DeltaAction action) {
        this.keyInput = Objects.requireNonNull(input);
        this.trigger = Objects.requireNonNull(trigger);
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

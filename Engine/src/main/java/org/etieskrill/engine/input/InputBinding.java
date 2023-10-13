package org.etieskrill.engine.input;

import org.etieskrill.engine.input.action.Action;

import java.util.Objects;

public class InputBinding {
    
    private final KeyInput keyInput;
    private final Trigger trigger;
    private final Action action;
    private final OverruleGroup group;
    
    public enum Trigger {
        ON_PRESS,
        PRESSED,
        ON_TOGGLE,
        TOGGLED
    }
    
    public InputBinding(KeyInput input, Trigger trigger, Action action, OverruleGroup group) {
        this.keyInput = Objects.requireNonNull(input);
        this.trigger = Objects.requireNonNull(trigger);
        this.action = Objects.requireNonNull(action);
        this.group = group;
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
    
    public OverruleGroup getGroup() {
        return group;
    }
    
}

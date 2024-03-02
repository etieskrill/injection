package org.etieskrill.engine.input;

import org.etieskrill.engine.input.action.Action;

import java.util.Objects;

public class InputBinding {

    private final Key key;
    private final Trigger trigger;
    private final Action action;
    private final OverruleGroup group;
    
    public enum Trigger {
        ON_PRESS,
        PRESSED,
        //TODO add ON_RELEASE and RELEASED events
        ON_TOGGLE,
        TOGGLED
    }

    public InputBinding(Key input, Trigger trigger, Action action, OverruleGroup group) {
        this.key = Objects.requireNonNull(input);
        this.trigger = Objects.requireNonNull(trigger);
        this.action = Objects.requireNonNull(action);
        this.group = group;
    }

    public Key getInput() {
        return key;
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

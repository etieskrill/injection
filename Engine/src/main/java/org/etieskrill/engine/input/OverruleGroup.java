package org.etieskrill.engine.input;

import java.util.Set;

/**
 * Defines an activation condition over a distinct group of input bindings.
 */
//TODO define release behaviour
public class OverruleGroup {
    
    private final Set<KeyInput> group;
    private final Mode mode;
    
    /**
     * Defines the behaviour of the group.
     */
    public enum Mode {
        /**
         * Causes a new group input to release an active one, if any.
         */
        //TODO i'd like to rename this to latest, but what is the corresponding "oldest" that doesn't sound awkward?
        YOUNGEST,
        /**
         * Blocks any group input from activating until no input is active.
         */
        OLDEST,
        /**
         * Allows all inputs to activate simultaneously, effectively disabling the group.
         */
        ALL,
        /**
         * Allows only a single active input, releasing the whole group in case of conflicting input.
         */
        NONE
    }
    
    public OverruleGroup(Set<KeyInput> group, Mode mode) {
        this.group = group;
        this.mode = mode;
    }
    
    public Set<KeyInput> getGroup() {
        return group;
    }
    
    public Mode getMode() {
        return mode;
    }
    
}

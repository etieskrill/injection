package org.etieskrill.engine.input

/**
 * Defines the behaviour of the group.
 */
enum class OverruleMode {
    /**
     * Causes a new group input to release an active one, if any.
     */
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

/**
 * Defines an activation condition over a distinct group of input bindings.
 */
//TODO define release behaviour
class OverruleGroup(
    val group: Set<KeyEvent>, //abusing the event for convenience
    val mode: OverruleMode
)

package org.etieskrill.engine.input

enum class InputTriggerEdge {
    ON_PRESS,
    PRESSED,
    //TODO add ON_RELEASE and RELEASED events
    ON_TOGGLE,
    TOGGLED
}

data class InputBinding(
    val key: KeyEvent, //abusing the event for convenience
    val trigger: InputTriggerEdge,
    val group: OverruleGroup? = null,
    val action: (Double) -> Any?
)

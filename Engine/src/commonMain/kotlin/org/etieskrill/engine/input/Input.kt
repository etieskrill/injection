package org.etieskrill.engine.input

fun inputs(vararg bindings: InputBinding) = KeyInputManager().addBindings(*bindings)

fun bindKey(
    key: Key,
    modifiers: Set<ModifierKey> = emptySet(),
    on: InputTriggerEdge = InputTriggerEdge.ON_PRESS,
    group: OverruleGroup? = null,
    to: (Double) -> Any
) = InputBinding(KeyEvent(key, KeyEventAction.PRESS, modifiers.toList()), on, group, to)

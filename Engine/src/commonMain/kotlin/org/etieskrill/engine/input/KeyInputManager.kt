package org.etieskrill.engine.input

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

open class KeyInputManager(
    private val bindings: MutableMap<KeyEvent, TriggerAction> = mutableMapOf(),
    private val groups: MutableMap<KeyEvent, OverruleGroup> = mutableMapOf()
) : KeyInputHandler {

    //TODO add <name,action> map to allow for rebinding and structural streamlining
    //TODO allow for keys to overrule/bind each other with activation policies; latest, oldest, all, none

    private val groupKeysActive = mutableMapOf<OverruleGroup, Int>()

    private val pressed = mutableSetOf<KeyEvent>()
    private val toggled = mutableSetOf<KeyEvent>()
    private val events = mutableListOf<KeyEvent>()

    data class TriggerAction(
        val trigger: InputTriggerEdge,
        val action: (Double) -> Any?
    )

    fun addBindings(vararg bindings: InputBinding) = bindings.forEach {
        this.bindings[it.key] = TriggerAction(it.trigger, it.action)
        it.group?.let { group -> addGroups(group) }
    }

    fun removeBindings(vararg bindings: InputBinding) = bindings.forEach {
        this.bindings -= it.key
        //TODO remove groups too?
    }

    fun addGroups(vararg groups: OverruleGroup) = groups.forEach { group ->
        group.group.forEach { key ->
            if (this.groups.put(key, group) != null) {
                logger.warn {
                    "The binding $key is present in multiple groups, only the last group registered will be respected."
                }
            }
        }
    }

    open fun update(delta: Double) {
        pressed.forEach { event -> bindings[event]?.let { if (it.trigger == InputTriggerEdge.PRESSED) it.action(delta) } }
        toggled.forEach { event -> bindings[event]?.let { if (it.trigger == InputTriggerEdge.TOGGLED) it.action(delta) } }

        events.forEach { event -> bindings[event]?.action(delta) }
        events.clear()
    }

    override fun invoke(event: KeyEvent): Boolean {
        if (event.action == KeyEventAction.REPEAT) return false //omitted for simplicity - for now

        val triggerAction = bindings[event]

        var handled = false

        //queue press trigger event
        if (triggerAction != null && triggerAction.trigger == InputTriggerEdge.ON_PRESS) {
            if (event.action == KeyEventAction.PRESS && !pressed.contains(event)) {
                events += event
                handled = true
            }
        }

        //queue toggle trigger event
        if (triggerAction != null) {
            if (triggerAction.trigger == InputTriggerEdge.ON_TOGGLE &&
                event.action == KeyEventAction.PRESS && !toggled.contains(event)
            ) {
                events += event
                handled = true
            }
        }

        //update toggle status
        if (event.action == KeyEventAction.PRESS && !pressed.contains(event)) {
            if (!toggled.contains(event)) toggled += event
            else toggled -= event
        }

        //update press status
        if (event.action != KeyEventAction.RELEASE) {
            groups[event]
                ?.let { handleOverrule(it, event) }
                ?: pressed.add(event)
        } else {
            pressed.removeAll { it.key == event.key }
        }

        //TODO finish implementation of OverruleGroup.Mode#NONE
        groups[event]?.let {
            groupKeysActive.compute(it) { _, count ->
                if (event.action != KeyEventAction.RELEASE) (count ?: 0) + 1 else (count ?: 0) - 1
            }
        }

        return handled
    }

    private fun handleOverrule(group: OverruleGroup, key: KeyEvent) = when (group.mode) {
        OverruleMode.YOUNGEST -> {
            pressed.removeAll(group.group)
            pressed += key
        }

        OverruleMode.OLDEST -> {
            if (group.group.none { it in pressed }) {
                pressed += key
            } else {
            }
        }

        OverruleMode.ALL -> pressed += key
        OverruleMode.NONE -> TODO()
    }

    fun isPressed(input: KeyEvent) = input in pressed
    fun isToggled(input: KeyEvent) = input in toggled

}

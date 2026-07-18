package org.etieskrill.engine.input

data class KeyEvent(
    val key: Key,
    val action: KeyEventAction,
    val modifiers: List<ModifierKey>,
) {

    fun withoutModifiers(): KeyEvent = KeyEvent(key, action, emptyList())

    override fun equals(other: Any?) = other is KeyEvent
            && key == other.key
            && action == other.action
            && key.mod == other.key.mod
            && (key.mod != null || modifiers != other.modifiers)

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + action.hashCode()

        if (key.mod != null) return result

        for (modifier in modifiers) {
            result = 31 * result + modifier.hashCode()
        }
        return result
    }

}

enum class KeyEventAction { PRESS, RELEASE, REPEAT }

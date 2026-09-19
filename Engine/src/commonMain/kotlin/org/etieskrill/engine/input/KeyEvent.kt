package org.etieskrill.engine.input

data class KeyEvent(
    val key: Key,
    val action: KeyEventAction,
    val modifiers: List<ModifierKey>,
) {

    fun withoutModifiers(): KeyEvent = KeyEvent(key, action, emptyList())

    override fun equals(other: Any?) = other is KeyEvent
            && key.aliasEquals(other.key)
            && action == other.action
            && key.mod == other.key.mod
            && (key.mod != null || modifiers == other.modifiers)

    override fun hashCode(): Int {
        val resolvedKey = key.alias?.alias ?: key.alias ?: key

        var result = resolvedKey.hashCode()
        result = 31 * result + action.hashCode()

        if (resolvedKey.mod != null) return result

        for (modifier in modifiers) {
            result = 31 * result + modifier.hashCode()
        }
        return result
    }

}

enum class KeyEventAction { PRESS, RELEASE, REPEAT }

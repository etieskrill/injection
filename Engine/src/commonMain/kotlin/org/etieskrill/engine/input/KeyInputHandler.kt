package org.etieskrill.engine.input

/**
 * Accepts generic input events from any input method which can provide either `PRESS` or `RELEASE`.
 */
fun interface KeyInputHandler {

    fun invoke(event: KeyEvent): Boolean

    fun invokeCharacter(character: Char): Boolean = false

}

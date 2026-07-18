package org.etieskrill.engine.input

interface CursorInputHandler {

    fun invokeClick(button: KeyEvent, posX: Double, posY: Double): Boolean

    fun invokeMove(posX: Double, posY: Double): Boolean

    fun invokeScroll(deltaX: Double, deltaY: Double): Boolean

}

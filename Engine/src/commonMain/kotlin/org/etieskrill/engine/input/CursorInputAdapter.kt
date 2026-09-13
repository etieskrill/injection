package org.etieskrill.engine.input

interface CursorInputAdapter : CursorInputHandler {

    override fun invokeClick(button: KeyEvent, posX: Double, posY: Double): Boolean = false

    override fun invokeMove(posX: Double, posY: Double): Boolean = false

    override fun invokeScroll(deltaX: Double, deltaY: Double): Boolean = false

}

package org.etieskrill.engine.input

abstract class MouseGestureHandler(val primaryMouseKey: Keys = Keys.LEFT_MOUSE) : CursorInputHandler {

    private var primaryMouseKeyDown: Boolean = false

    private var prevX: Double = 0.0
    private var prevY: Double = 0.0

    override fun invokeClick(
        button: Key?,
        action: Keys.Action,
        posX: Double,
        posY: Double
    ): Boolean {
        if (button == primaryMouseKey.input) {
            primaryMouseKeyDown = action != Keys.Action.RELEASE
            prevX = posX
            prevY = posY
        }
        return false
    }

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        if (!primaryMouseKeyDown) return false

        val handled = invokeDrag(prevX - posX, prevY - posY, prevX, prevY)

        prevX = posX
        prevY = posY

        return handled
    }

    abstract fun invokeDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean

    override fun invokeScroll(deltaX: Double, deltaY: Double): Boolean = false

}

package org.etieskrill.engine.window

import org.etieskrill.engine.common.Disposable
import org.joml.Vector2d

expect class Cursor : Disposable {

    constructor(shape: CursorShape = CursorShape.ARROW)

    var mode: CursorMode
    var shape: CursorShape

    var position: Vector2d

    fun enable()
    fun disable()
    fun hide()
    fun capture()

}

enum class CursorShape {
    ARROW,
    IBEAM,
    CROSSHAIR,
    POINTING_HAND,
    RESIZE_EW,
    RESIZE_NS,
    RESIZE_NWSE,
    RESIZE_NESW,
    RESIZE_ALL,
    NOT_ALLOWED;
}

enum class CursorMode {
    /**
     * Movement is not restricted and normal cursor is shown.
     */
    NORMAL,

    /**
     * Movement is not restricted, but the cursor is not visible while hovering over the window.
     */
    HIDDEN,

    /**
     * Cursor movement is locked to the window, and movement is fed into a virtual unlimited cursor space. Use
     * for mouse motion based camera control and the likes.
     */
    DISABLED,

    /**
     * Restricts cursor movement to the window, but otherwise behaves normally.
     */
    CAPTURED;
}

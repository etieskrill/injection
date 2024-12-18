package org.etieskrill.game.horde.component

class MovementSpeed(var value: Float = 1f, var factor: Float = 1f) {
    val speed
        get() = this.value * this.factor
}

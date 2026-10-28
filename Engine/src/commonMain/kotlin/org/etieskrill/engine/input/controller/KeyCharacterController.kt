package org.etieskrill.engine.input.controller

import net.bifreus.kml.Vec3
import org.etieskrill.engine.input.InputTriggerEdge
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.input.bindKey

/**
 * @param T the type of object being controlled
 */
open class KeyCharacterController<T>(
    protected val target: T,
    protected val updateFunction: Updatable<T>,

    var speed: Float = 1f,
    var updateCondition: (() -> Boolean)? = null,
) : KeyInputManager() {

    protected val deltaPosition = Vec3()

    init {
        addBindings(
            bindKey(Key.W, on = InputTriggerEdge.PRESSED) { deltaPosition.z += 1 },
            bindKey(Key.S, on = InputTriggerEdge.PRESSED) { deltaPosition.z -= 1 },
            bindKey(Key.A, on = InputTriggerEdge.PRESSED) { deltaPosition.x += 1 },
            bindKey(Key.D, on = InputTriggerEdge.PRESSED) { deltaPosition.x -= 1 },
            bindKey(Key.SPACE, on = InputTriggerEdge.PRESSED) { deltaPosition.y += 1 },
            bindKey(Key.SHIFT, on = InputTriggerEdge.PRESSED) { deltaPosition.y -= 1 }
        )
    }

    fun interface Updatable<T> {
        fun update(delta: Double, target: T, deltaPosition: Vec3, speed: Float)
    }

    override fun update(delta: Double) {
        super.update(delta)
        if (updateCondition?.invoke() ?: true) {
            updateFunction.update(delta, target, deltaPosition, speed)
        }
        deltaPosition(0)
    }

}

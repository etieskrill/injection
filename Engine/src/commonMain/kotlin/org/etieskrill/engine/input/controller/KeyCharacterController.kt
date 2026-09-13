package org.etieskrill.engine.input.controller

import org.etieskrill.engine.input.InputTriggerEdge
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.input.bindKey
import org.joml.Vector3f

/**
 * @param T the type of object being controlled
 */
open class KeyCharacterController<T>(
    protected val target: T,
    protected val updateFunction: Updatable<T>,

    var speed: Float = 1f,
    var updateCondition: (() -> Boolean)? = null,
) : KeyInputManager() {

    protected val deltaPosition = Vector3f()

    init {
        addBindings(
            bindKey(Key.W, on = InputTriggerEdge.PRESSED) { deltaPosition.add(0f, 0f, 1f) },
            bindKey(Key.S, on = InputTriggerEdge.PRESSED) { deltaPosition.add(0f, 0f, -1f) },
            bindKey(Key.A, on = InputTriggerEdge.PRESSED) { deltaPosition.add(-1f, 0f, 0f) },
            bindKey(Key.D, on = InputTriggerEdge.PRESSED) { deltaPosition.add(1f, 0f, 0f) },
            bindKey(Key.SPACE, on = InputTriggerEdge.PRESSED) { deltaPosition.add(0f, 1f, 0f) },
            bindKey(Key.SHIFT, on = InputTriggerEdge.PRESSED) { deltaPosition.add(0f, -1f, 0f) }
        )
    }

    fun interface Updatable<T> {
        fun update(delta: Double, target: T, deltaPosition: Vector3f, speed: Float)
    }

    override fun update(delta: Double) {
        super.update(delta)
        if (updateCondition?.invoke() ?: true) {
            updateFunction.update(delta, target, deltaPosition, speed)
        }
        deltaPosition.zero()
    }

}

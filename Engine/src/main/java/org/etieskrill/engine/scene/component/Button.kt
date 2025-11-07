package org.etieskrill.engine.scene.component

import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.component.container.Container
import org.joml.timesAssign

typealias SimpleAction = suspend () -> Unit

/**
 * A node with a single child, which runs an action when hit.
 * <p>
 * This intercepts the event, i.e. the hit is not propagated to the child.
 */
class Button(
    child: Node<*> = Label("<no content>"),
    private var enabled: Boolean = true,
    var action: SimpleAction = {}
) : Container(child) {

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        if (!enabled || !doesHit(posX, posY)) return false
        if (action == Keys.Action.RELEASE
            && button == Keys.LEFT_MOUSE.input
        ) {
            ui { action() }
            return true
        }
        return false
    }

    fun enable() {
        enabled = true
        renderedColour.set(colour)
        child?.let { renderedColour.set(colour) }
    }

    fun disable() {
        enabled = false
        renderedColour.set(colour) *= 0.75f //thats... pretty cursed
        child?.let { renderedColour.set(colour) *= 0.75f }
    }

}

package org.etieskrill.engine.scene.component.container

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.component.Node
import org.etieskrill.engine.scene.component.getPreferredNodePosition
import org.joml.Vector3f

/**
 * A node with a single child.
 */
open class Container(child: Node<*>?) : Node<Container>() {

    var child: Node<*>? = child
        set(value) {
            invalidate()
            child?.parent = this
            field = value
        }

    init {
        this.child = child //setter is not called on property initialisation
    }

    override fun update(delta: Double) {
        child?.update(delta)
    }

    override fun format() {
        if (!shouldFormat()) return

        child?.run {
            format()
            position = getPreferredNodePosition(size, this)
        }
    }

    override fun render(batch: Batch) {
        if (renderedColour.w != 0f) {
            batch.renderBox(
                Vector3f(position, 0f),
                Vector3f(size, 0f),
                renderedColour
            )
        }

        child?.render(batch)
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double) = when {
        !doesHit(posX, posY) -> false
        child == null -> false
        else -> child!!.handleHit(button, action, posX, posY)
    }

    override fun handleKey(key: Key, action: Keys.Action) =
        child?.handleKey(key, action) ?: false

    override fun handleHover(posX: Double, posY: Double) = when {
        !doesHit(posX, posY) -> false
        child == null -> false
        else -> child!!.handleHover(posX, posY)
    }

    override fun handleDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double) = when {
        !doesHit(posX, posY) -> false
        child == null -> false
        else -> child!!.handleDrag(deltaX, deltaY, posX, posY)
    }

}
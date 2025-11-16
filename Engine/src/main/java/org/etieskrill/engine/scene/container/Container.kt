package org.etieskrill.engine.scene.container

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.getPreferredNodePosition
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

    override fun computeFixedSizes() {
        if (!shouldFormat) return

        child?.let { child ->
            child.computeFixedSizes()

            when (scaleMode) {
                ScaleMode.FIXED -> {
                    computedFixedSize = true
                    formattedSize.set(size)
                }

                ScaleMode.CONTENT -> {
                    if (child.scaleMode == ScaleMode.GROW
                        || child.computedFixedSize == false
                    ) {
                        computedFixedSize = false
                        return
                    }

                    formattedSize.set(child.formattedSize)
                    computedFixedSize = true
                }

                ScaleMode.GROW -> computedFixedSize = false
            }
        }
    }

    override fun layout() {
        if (!shouldFormat()) return

        child?.run {
            if (scaleMode == ScaleMode.GROW) {
                formattedSize = this@Container.formattedSize
                computedFixedSize = true
            }
            layout()
            position = getPreferredNodePosition(this@Container.formattedSize, this)
        }
    }

    override fun render(batch: Batch) {
        if (renderedColour.w != 0f) {
            batch.renderBox(
                Vector3f(absolutePosition, 0f),
                Vector3f(formattedSize, 0f),
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
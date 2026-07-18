package org.etieskrill.engine.scene.container

import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.scene.Batch
import org.joml.Vector3f

/**
 * A node with a single child.
 */
open class Container(child: org.etieskrill.engine.scene.Node<*>? = null) : org.etieskrill.engine.scene.Node<Container>() {

    var child: org.etieskrill.engine.scene.Node<*>? = child
        set(value) {
            invalidate()
            value?.parent = this
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
                    formattedSize = size
                }

                ScaleMode.CONTENT -> {
                    if (child.scaleMode == ScaleMode.GROW
                        || !child.computedFixedSize
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
            position = _root_ide_package_.org.etieskrill.engine.scene.getPreferredNodePosition(
                this@Container.formattedSize,
                this
            )
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

    override fun handleHit(event: KeyEvent, posX: Double, posY: Double) = when {
        !doesHit(posX, posY) -> false
        child == null -> false
        else -> child!!.handleHit(event, posX, posY)
    }

    override fun handleKey(event: KeyEvent) =
        child?.handleKey(event) ?: false

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
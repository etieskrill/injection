package org.etieskrill.engine.scene.container

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.Node.Alignment.FIXED_POSITION
import org.etieskrill.engine.scene.Node.ScaleMode.*
import org.etieskrill.engine.scene.getPreferredNodePosition
import org.joml.Vector2f
import org.joml.minus
import org.joml.plus

/**
 * A node with any number of children, whose layouts are respected independently of each other.
 */
open class Stack(
    protected val children: MutableList<Node<*>>
) : Node<Stack>() {

    constructor(vararg children: Node<*>) : this(children.toMutableList())
    constructor() : this(mutableListOf())

    override fun update(delta: Double) {
        children.forEach { it.update(delta) }
    }

    override fun computeFixedSizes() {
        if (!shouldFormat) return

        children.forEach { it.computeFixedSizes() }

        when (scaleMode) {
            FIXED -> {
                computedFixedSize = true
                formattedSize = size
                layout()
            }

            CONTENT -> {
                if (children.any { it.scaleMode == GROW || !it.computedFixedSize }) {
                    computedFixedSize = false
                    return
                }

                computeBoundingBox()
                computedFixedSize = true
                layout()
            }

            GROW -> computedFixedSize = false
        }
    }

    /**
     * Guaranteed to be called only when all children have either fixed or computed size.
     */
    protected open fun computeBoundingBox() {
        if (children.isEmpty()) {
            formattedSize.set(0f)
            return
        }

        val min = Vector2f()
        val max = Vector2f()

        children.forEachIndexed { i, child ->
            if (i == 0) {
                min.set(child.position)
                max.set(child.position + child.size)
                return@forEachIndexed
            }

            min.min(child.position)
            max.max(child.position + child.size)
        }

        formattedSize = max - min
    }

    override fun layout() {
        if (!shouldFormat()) return

        if (parent == null && scaleMode != FIXED) {
            throw IllegalStateException("Scale mode for root node must be FIXED")
        }

        children.forEach { child ->
            child.layout()
            when (child.scaleMode) {
                FIXED -> {}
                CONTENT -> {
                    if (!child.computedFixedSize) { //growing child inside
                        child.formattedSize = formattedSize
                    }
                }

                GROW -> child.formattedSize = formattedSize
            }

            if (child.alignment != FIXED_POSITION) {
                child.position = getPreferredNodePosition(formattedSize, child)
            }
        }
    }

    override fun render(batch: Batch) {
        children.forEach { it.render(batch) }
    }

    fun addChildren(vararg children: Node<*>) {
        for (child in children) {
            child.parent = this
            this.children += child
        }
    }

    fun setChild(index: Int, child: Node<*>) {
        child.parent = this
        children[index] = child
    }

    fun removeChildren(vararg children: Node<*>) {
        for (child in children) {
            child.parent = null
            this.children -= child
        }
    }

    fun clearChildren() {
        children.forEach { it.parent = null }
        children.clear()
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false
        children.forEach { child ->
            if (child.handleHit(button, action, posX, posY)) {
                return true
            }
        }
        return false
    }

    override fun handleKey(key: Key, action: Keys.Action): Boolean {
        children.forEach { child ->
            if (child.handleKey(key, action)) {
                return true
            }
        }
        return false
    }

    override fun handleHover(posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false
        children.forEach { child ->
            if (child.handleHover(posX, posY)) {
                return true
            }
        }
        return false
    }

    override fun handleDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false
        children.forEach { child ->
            if (child.handleDrag(deltaX, deltaY, posX, posY)) {
                return true
            }
        }
        return false
    }

}

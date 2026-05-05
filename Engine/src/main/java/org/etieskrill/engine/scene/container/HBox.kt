package org.etieskrill.engine.scene.container

import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.Node.Alignment.*
import org.etieskrill.engine.scene.Node.ScaleMode.GROW
import org.etieskrill.engine.scene.getPreferredNodePosition
import org.etieskrill.engine.scene.minNodeSize
import kotlin.math.max

open class HBox(
    children: List<Node<*>>
) : Stack(children) {

    constructor(vararg children: Node<*>) : this(children.toList())
    constructor() : this(mutableListOf())

    override fun computeBoundingBox() {
        var width = 0f
        var height = 0f

        for (child in children) {
            val nodeSize = child.minNodeSize
            width += nodeSize.x
            height = max(height, nodeSize.y)
        }

        formattedSize.apply { x = width; y = height }
    }

    override fun layout() {
        if (!shouldFormat()) return

        //Pre-calculate the size of the smallest fitting box around the children and position cursors accordingly
        var leftPointer = 0f
        var centerPointer = formattedSize.x / 2f
        var rightPointer = formattedSize.x
        var numLeftGrow = 0
        var numCenterGrow = 0
        var numRightGrow = 0
        children.forEachIndexed { i, child ->
            if (child.scaleMode == GROW) {
                when (child.alignment) {
                    TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> numLeftGrow++
                    TOP, CENTER, BOTTOM -> numCenterGrow++
                    TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> numRightGrow++
                    FIXED_POSITION -> {}
                }
                return@forEachIndexed
            }

            child.computeFixedSizes()

            if (child.alignment == FIXED_POSITION) return@forEachIndexed

            val margin = children.getOrNull(i + 1)
                ?.let { max(it.margin.z, child.margin.w) }
                ?: 0f

            when (child.alignment) {
                TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> leftPointer += child.minNodeSize.x - margin
                TOP, CENTER, BOTTOM -> centerPointer -= child.formattedSize.x / 2f
                TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> rightPointer -= child.minNodeSize.x - margin
                FIXED_POSITION -> {}
            }
        }

        val leftGrowCapacity = if (numLeftGrow > 0) (formattedSize.x - leftPointer) / numLeftGrow else 0f
        val centerGrowCapacity = if (numCenterGrow > 0) (formattedSize.x - centerPointer) * 2 / numCenterGrow else 0f
        val rightGrowCapacity =
            if (numRightGrow > 0) (formattedSize.x - (formattedSize.x - rightPointer)) / numRightGrow else 0f

        leftPointer = 0f

        //Place children ignoring vertical preference and adjust cursors
        children.forEachIndexed { i, child ->
            if (child.scaleMode == GROW) {
                child.formattedSize.apply {
                    x = when (child.alignment) {
                        TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> leftGrowCapacity
                        TOP, CENTER, BOTTOM -> centerGrowCapacity
                        TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> rightGrowCapacity
                        FIXED_POSITION -> child.formattedSize.x
                    }
                    y = formattedSize.y
                }
                child.computedFixedSize = true
            }
            child.layout()

            if (child.alignment == FIXED_POSITION) return@forEachIndexed

            child.position = getPreferredNodePosition(formattedSize, child).apply {
                x = when (child.alignment) {
                    FIXED_POSITION -> 0f
                    TOP, TOP_LEFT, TOP_RIGHT -> leftPointer
                    CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer
                    BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> rightPointer
                }
            }

            val margin = children.getOrNull(i + 1)
                ?.let { max(it.margin.z, it.margin.w) }
                ?: 0f

            val childWidth = child.formattedSize.x
            when (child.alignment) {
                TOP, TOP_LEFT, TOP_RIGHT -> leftPointer += childWidth + margin
                CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childWidth + margin
                BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> rightPointer += childWidth + margin
                FIXED_POSITION -> {}
            }
        }

        if (children.any { !it.computedFixedSize }) {
            throw IllegalStateException("Absolute size could not be computed for all children")
        }
    }

}

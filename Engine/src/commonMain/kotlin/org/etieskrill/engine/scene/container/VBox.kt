package org.etieskrill.engine.scene.container

import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.Node.Alignment.*
import org.etieskrill.engine.scene.Node.ScaleMode.GROW
import org.etieskrill.engine.scene.getPreferredNodePosition
import org.etieskrill.engine.scene.minNodeSize
import kotlin.math.max

open class VBox(
    children: List<Node<*>>
) : Stack(children) {

    constructor(vararg children: Node<*>) : this(children.toList())
    constructor() : this(mutableListOf())

    override fun computeBoundingBox() {
        var width = 0f
        var height = 0f

        for (child in children) {
            val nodeSize = child.minNodeSize
            height += nodeSize.y
            width = max(width, nodeSize.x)
        }

        formattedSize.apply { x = width; y = height }
    }

    override fun layout() {
        if (!shouldFormat()) return

        //Pre-calculate the size of the smallest fitting box around the children and position cursors accordingly
        var topPointer = 0f
        var centerPointer = formattedSize.y / 2f
        var bottomPointer = formattedSize.y
        var numTopGrow = 0
        var numCenterGrow = 0
        var numBottomGrow = 0
        children.forEachIndexed { i, child ->
            if (child.scaleMode == GROW) {
                when (child.alignment) {
                    TOP_LEFT, TOP, TOP_RIGHT -> numTopGrow++
                    CENTER, CENTER_LEFT, CENTER_RIGHT -> numCenterGrow++
                    BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> numBottomGrow++
                    FIXED_POSITION -> {}
                }
                return@forEachIndexed
            }

            child.computeFixedSizes()

            if (child.alignment == FIXED_POSITION) return@forEachIndexed

            val margin = children.getOrNull(i + 1)
                ?.let { max(it.margin.y, it.margin.x) }
                ?: 0f

            when (child.alignment) {
                TOP_LEFT, TOP, TOP_RIGHT -> topPointer += child.minNodeSize.y - margin
                CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer -= child.formattedSize.y / 2f
                BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer -= child.minNodeSize.y - margin
                FIXED_POSITION -> {}
            }
        }

        val topGrowCapacity = if (numTopGrow > 0) (formattedSize.y - topPointer) / numTopGrow else 0f
        val centerGrowCapacity = if (numCenterGrow > 0) (formattedSize.y - centerPointer) * 2 / numCenterGrow else 0f
        val bottomGrowCapacity =
            if (numBottomGrow > 0) (formattedSize.y - (formattedSize.y - bottomPointer)) / numBottomGrow else 0f

        topPointer = 0f

        //Place children ignoring vertical preference and adjust cursors
        children.forEachIndexed { i, child ->
            if (child.scaleMode == GROW) {
                child.formattedSize.apply {
                    x = formattedSize.x
                    y = when (child.alignment) {
                        TOP_LEFT, TOP, TOP_RIGHT -> topGrowCapacity
                        CENTER_LEFT, CENTER, CENTER_RIGHT -> centerGrowCapacity
                        BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> bottomGrowCapacity
                        FIXED_POSITION -> child.formattedSize.y
                    }
                }
                child.computedFixedSize = true
            }
            child.layout()

            if (child.alignment == FIXED_POSITION) return@forEachIndexed
            child.position = getPreferredNodePosition(formattedSize, child).apply {
                y = when (child.alignment) {
                    FIXED_POSITION -> 0f
                    TOP, TOP_LEFT, TOP_RIGHT -> topPointer
                    CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer
                    BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer
                }
            }

            val margin = children.getOrNull(i + 1)
                ?.let { max(it.margin.y, it.margin.x) }
                ?: 0f

            val childHeight = child.formattedSize.y
            when (child.alignment) {
                TOP, TOP_LEFT, TOP_RIGHT -> topPointer += childHeight + margin
                CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childHeight + margin
                BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer += childHeight + margin
                FIXED_POSITION -> {}
            }
        }

        if (children.any { !it.computedFixedSize }) {
            throw IllegalStateException("Absolute size could not be computed for all children")
        }
    }

}

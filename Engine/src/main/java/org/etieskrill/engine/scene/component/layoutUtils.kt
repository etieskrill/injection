package org.etieskrill.engine.scene.component

import org.etieskrill.engine.scene.component.Node.Alignment.*
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.minus
import org.joml.plus
import org.joml.times

internal fun getMinNodeSize(node: Node<*>): Vector2f =
    node.formattedSize + node.margin.run { Vector2f(z + w, x + y) }

internal fun getPreferredNodePosition(size: Vector2fc, node: Node<*>): Vector2f {
    val nodeSize = node.formattedSize + Vector2f(node.margin.w, node.margin.y) * 2f
    val marginPos = Vector2f(node.margin.z, node.margin.x)

    val size = Vector2f(size)
    val pos = when (node.alignment) {
        FIXED_POSITION -> return node.position

        TOP_LEFT -> Vector2f(0f)
        TOP -> (size - nodeSize) * Vector2f(0.5f, 0f)
        TOP_RIGHT -> (size - nodeSize) * Vector2f(1f, 0f)
        CENTER_LEFT -> (size - nodeSize) * Vector2f(0f, 0.5f)
        CENTER -> (size * 0.5f) - (nodeSize * 0.5f)
        CENTER_RIGHT -> (size - nodeSize) * Vector2f(1f, 0.5f)
        BOTTOM_LEFT -> (size - nodeSize) * Vector2f(0f, 1f)
        BOTTOM -> (size - nodeSize) * Vector2f(0.5f, 1f)
        BOTTOM_RIGHT -> size - nodeSize
    }

    return pos + marginPos
}

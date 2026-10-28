package org.etieskrill.engine.scene

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import org.etieskrill.engine.scene.Node.Alignment.*

internal val Node<*>.minNodeSize get() = this.formattedSize + this.margin.run { Vec2(z + w, x + y) }

internal fun getPreferredNodePosition(size: Vec2c, node: Node<*>): Vec2 {
    val nodeSize = node.formattedSize + node.margin.wy * 2f
    val marginPos = node.margin.zx

    val size = Vec2(size)
    val pos = when (node.alignment) {
        FIXED_POSITION -> return node.position

        TOP_LEFT -> Vec2(0f)
        TOP -> (size - nodeSize) * Vec2(0.5f, 0f)
        TOP_RIGHT -> (size - nodeSize) * Vec2(1f, 0f)
        CENTER_LEFT -> (size - nodeSize) * Vec2(0f, 0.5f)
        CENTER -> (size * 0.5f) - (nodeSize * 0.5f)
        CENTER_RIGHT -> (size - nodeSize) * Vec2(1f, 0.5f)
        BOTTOM_LEFT -> (size - nodeSize) * Vec2(0f, 1f)
        BOTTOM -> (size - nodeSize) * Vec2(0.5f, 1f)
        BOTTOM_RIGHT -> size - nodeSize
    }

    return pos + marginPos
}

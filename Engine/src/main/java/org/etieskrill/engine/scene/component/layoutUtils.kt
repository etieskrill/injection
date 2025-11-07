package org.etieskrill.engine.scene.component

import org.etieskrill.engine.scene.component.Node.Alignment.BOTTOM
import org.etieskrill.engine.scene.component.Node.Alignment.BOTTOM_LEFT
import org.etieskrill.engine.scene.component.Node.Alignment.BOTTOM_RIGHT
import org.etieskrill.engine.scene.component.Node.Alignment.CENTER
import org.etieskrill.engine.scene.component.Node.Alignment.CENTER_LEFT
import org.etieskrill.engine.scene.component.Node.Alignment.CENTER_RIGHT
import org.etieskrill.engine.scene.component.Node.Alignment.FIXED_POSITION
import org.etieskrill.engine.scene.component.Node.Alignment.TOP
import org.etieskrill.engine.scene.component.Node.Alignment.TOP_LEFT
import org.etieskrill.engine.scene.component.Node.Alignment.TOP_RIGHT
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.minus
import org.joml.plus
import org.joml.times

//internal fun getMinNodeSize(node: Node<*>): Vector2f =
//    node.size + node.margin.run { Vector2f(z + w, x + y) }
//
//internal fun getPreferredNodePosition(size: Vector2fc, node: Node<*>): Vector2f {
//    val nodeSize = node.size + Vector2f(node.margin.w, node.margin.y) * 2f
//    val marginPos = Vector2f(node.margin.z, node.margin.x)
//
//    val pos = when (node.alignment) {
//        FIXED_POSITION -> return node.position
//
//        TOP_LEFT -> Vector2f(0f)
//        TOP -> (size - nodeSize) * Vector2f(0.5f, 0f)
//        TOP_RIGHT -> (size - nodeSize) * Vector2f(1f, 0f)
//        CENTER_LEFT -> (size - nodeSize) * Vector2f(0f, 0.5f)
//        CENTER -> (size * 0.5f) - (nodeSize * 0.5f)
//        CENTER_RIGHT -> (size - nodeSize) * Vector2f(1f, 0.5f)
//        BOTTOM_LEFT -> (size - nodeSize) * Vector2f(0f, 1f)
//        BOTTOM -> (size - nodeSize) * Vector2f(0.5f, 1f)
//        BOTTOM_RIGHT -> size - nodeSize
//    }
//
//    return pos + marginPos
//}

fun getMinNodeSize(node: Node<*>): Vector2f {
    val margin = node.margin
    return Vector2f(node.size)
        .add(margin.z() + margin.w(), margin.x() + margin.y());
}

fun getPreferredNodePosition(size: Vector2f, node: Node<*>): Vector2f {
    val nodeSize = Vector2f(node.size).add(
        Vector2f(node.margin.w(), node.margin.y())
            .mul(2f)
    )

    val marginPos = Vector2f(
        node.margin.z(),
        node.margin.x()
    )

    val _size = Vector2f(size)
//    if (node.alignment == FIXED_POSITION) {
//        return node.position
//    }
    val pos = when (node.alignment) {
        FIXED_POSITION -> return node.position

        TOP_LEFT -> Vector2f(0f, 0f)
        TOP -> _size.sub(nodeSize).mul(0.5f, 0f)
        TOP_RIGHT -> _size.sub(nodeSize).mul(1f, 0f)
        CENTER_LEFT -> _size.sub(nodeSize).mul(0f, 0.5f)
        CENTER -> _size.mul(0.5f).sub(nodeSize.mul(0.5f))
        CENTER_RIGHT -> _size.sub(nodeSize).mul(1f, 0.5f)
        BOTTOM_LEFT -> _size.sub(nodeSize).mul(0f, 1f)
        BOTTOM -> _size.sub(nodeSize).mul(0.5f, 1f)
        BOTTOM_RIGHT -> _size.sub(nodeSize)
    };

    return pos.add(marginPos);
}

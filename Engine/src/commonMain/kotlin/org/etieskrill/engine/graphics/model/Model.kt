package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.animation.Animation
import org.joml.primitives.AABBf

open class Model(
    val name: String,
    val rootNode: Node,
    val animations: List<Animation> = emptyList(),
    val bones: List<Bone> = emptyList(),
    val boundingBox: AABBf = AABBf()
) {

    /**
     * Depth-first flattened view of the node tree for use in animation systems.
     */
    internal val nodes = rootNode.getTree()
    val nodeCount get() = nodes.size

}

fun Node.getTree(list: MutableList<Node> = mutableListOf()): List<Node> {
    list += this
    children.forEach { it.getTree(list) }
    return list
}

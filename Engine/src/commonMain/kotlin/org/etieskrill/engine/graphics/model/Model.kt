package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.animation.Animation
import org.joml.primitives.AABBf

data class Model(
    val name: String,
    val rootNode: Node,
    val animations: List<Animation>,
    val bones: List<Bone>,
    val boundingBox: AABBf
) : Disposable {

    /**
     * Depth-first flattened view of the node tree for use in animation systems.
     */
    internal val nodes = rootNode.getTree()
    val nodeCount get() = nodes.size

    override fun dispose() = rootNode.dispose()

}

fun Node.getTree(list: MutableList<Node> = mutableListOf()): List<Node> {
    list += this
    children.forEach { it.getTree(list) }
    return list
}

package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.TransformC

data class Node(
    val name: String,
    val transform: TransformC,
    val parent: Node?,
    val children: MutableList<Node> = mutableListOf(),
    val meshes: List<Mesh>,
    val bone: Bone?
) : Disposable {

    fun getGlobalTransform(numNodesIgnoredFromRoot: Int = 0): Transform { //TODO precompute
        var root: Node? = this
        val nodes = mutableListOf<Node>()
        while (root != null) {
            nodes += root
            root = root.parent
        }

        nodes.dropLast(numNodesIgnoredFromRoot)

        val transform = Transform()
        nodes.asReversed().forEach { transform.apply(it.transform) }

        return transform
    }

    override fun dispose() {
        meshes.forEach(Mesh::dispose)
        children.forEach(Node::dispose)
    }

    override fun toString() = "Node{name='$name', transform=$transform, meshes=$meshes, bone=$bone}"

}

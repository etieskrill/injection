package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.TransformC
import org.etieskrill.engine.entity.component.applyInternal
import org.joml.times

data class Node(
    val name: String,
    val transform: TransformC = Transform(),
    val parent: Node? = null,
    val children: MutableList<Node> = mutableListOf(),
    val meshes: List<Mesh> = listOf(),
    val bone: Bone? = null
) {

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

    override fun toString() = "Node{name='$name', transform=$transform, meshes=$meshes, bone=$bone}"

}

package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.TransformC

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Node

        if (name != other.name) return false
        if (transform != other.transform) return false
        if (children != other.children) return false
        if (meshes != other.meshes) return false
        if (bone != other.bone) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + transform.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + meshes.hashCode()
        result = 31 * result + (bone?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "Node{name='$name', transform=$transform, meshes=$meshes, bone=$bone}"

}

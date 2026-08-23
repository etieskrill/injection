package org.etieskrill.engine.graphics.animation

import org.etieskrill.engine.graphics.model.Node

/**
 * A [NodeFilter] can be used to only apply an [Animation] to a select group of [Nodes][Node].
 *
 * E.g. a waving animation would only affect the nodes of one or both arms, which can then be either overridden or
 * interpolated with over the previous animation layer.
 */
class NodeFilter(val affectedNodes: Set<Node>) : (Node) -> Boolean {

    companion object {
        /**
         * Constructs a new [NodeFilter] which passes only the [nodes][Node] explicitly specified.
         *
         * @param nodes nodes to allow
         * @return a new filter passing only the nodes specified
         */
        fun explicit(vararg nodes: Node) = NodeFilter(setOf(*nodes))

        /**
         * Constructs a new [NodeFilter] which passes the [node][Node] specified, and all of its children in the
         * hierarchy.
         *
         * @param node node whose children (including itself) to allow
         * @return a new filter passing the node and its children
         */
        fun tree(node: Node): NodeFilter {
            val tree = mutableListOf<Node>()
            fillTree(node, tree)
            return NodeFilter(tree.toSet())
        }

        private fun fillTree(node: Node, nodes: MutableList<Node>) {
            nodes += node
            node.children.forEach { fillTree(it, nodes) }
        }
    }

    override fun invoke(node: Node) = affectedNodes.contains(node)

}

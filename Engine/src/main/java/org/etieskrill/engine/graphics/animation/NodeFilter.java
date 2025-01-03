package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Node;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@code NodeFilter} can be used to only apply an {@link Animation} to a select group of {@link Node Nodes}.
 * <p>
 * E.g. a waving animation would only affect the nodes of one or both arms, which can then be either overridden or
 * interpolated with over the previous animation layer.
 */
public class NodeFilter implements Predicate<Node> {

    private final Set<Node> affectedNodes;

    private NodeFilter(List<Node> affectedNodes) {
        this.affectedNodes = new HashSet<>();
        this.affectedNodes.addAll(affectedNodes);
    }

    /**
     * Constructs a new {@code NodeFilter} which passes only the {@link Node nodes} explicitly specified.
     *
     * @param nodes nodes to allow
     * @return a new filter passing only the nodes specified
     */
    public static NodeFilter explicit(Node... nodes) {
        return new NodeFilter(List.of(nodes));
    }

    /**
     * Constructs a new {@code NodeFilter} which passes the {@link Node node} specified, and all of its children in the
     * hierarchy.
     *
     * @param node node whose children (including itself) to allow
     * @return a new filter passing the node and its children
     */
    public static NodeFilter tree(Node node) {
        List<Node> tree = new ArrayList<>();
        fillTree(node, tree);
        return new NodeFilter(tree);
    }

    private static void fillTree(Node node, List<Node> nodes) {
        nodes.add(node);
        for (Node child : node.getChildren())
            fillTree(child, nodes);
    }

    @Override
    public boolean test(@Nullable Node node) {
        if (node == null) return false;
        return affectedNodes.contains(node);
    }

    public boolean allows(@Nullable Node node) {
        return test(node);
    }

}

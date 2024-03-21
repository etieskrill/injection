package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Node;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@code NodeFilter} can be used to only apply an {@link Animation} to a select group of {@link Node Nodes}.
 * <p>
 * E.g. a waving animation would only affect the nodes of one or both arms, which can then be either overridden or
 * interpolated with over the previous animation layer.
 */
public class NodeFilter implements Function<Node, Boolean> {

    private final Map<Node, Boolean> affectedNodes; //TODO dunno if a map really helps at all, i should go over this with a profiler first

    private NodeFilter(List<Node> affectedNodes) {
        this.affectedNodes = new HashMap<>();
        for (Node affectedNode : affectedNodes)
            this.affectedNodes.put(affectedNode, true);
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
    public Boolean apply(@Nullable Node node) {
        if (node == null) return false;
        return affectedNodes.get(node) != null;
    }

    public boolean allows(@Nullable Node node) {
        return apply(node);
    }

}

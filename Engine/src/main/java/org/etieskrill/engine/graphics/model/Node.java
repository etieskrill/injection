package org.etieskrill.engine.graphics.model;

import lombok.Getter;
import lombok.ToString;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Getter
@ToString(exclude = {"parent", "children"})
public final class Node implements Disposable {

    private final String name;
    private final TransformC transform;
    private final @Nullable Node parent;
    private final List<Node> children;
    private final List<Mesh> meshes;
    private final @Nullable Bone bone;

    public Node(String name, @Nullable Node parent, TransformC transform, List<Mesh> meshes, @Nullable Bone bone) {
        this.name = name;
        this.transform = transform;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.meshes = meshes;
        this.bone = bone;
    }

    public Transform getHierarchyTransform() {
        return getHierarchyTransform(0);
    }

    public Transform getHierarchyTransform(int numNodesIgnoredFromRoot) { //TODO precompute
        Node root = this;
        Stack<Node> line = new Stack<>();
        line.push(this);
        while ((root = root.getParent()) != null) //Traverse up the tree to the root, recording every node
            line.push(root);

        for (int i = 0; i < numNodesIgnoredFromRoot; i++) line.pop();

        Transform transform = new Transform();
        while (!line.isEmpty()) //Traverse down the tree, applying every transform in succession
            transform.apply(line.pop().getTransform());

        return transform;
    }

    @Override
    public void dispose() {
        meshes.forEach(Mesh::dispose);
        children.forEach(Node::dispose);
    }

}

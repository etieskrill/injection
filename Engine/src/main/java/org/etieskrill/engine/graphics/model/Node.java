package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.Transform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public final class Node implements Disposable {

    private final String name;
    private final Matrix4fc transform;
    private final @Nullable Node parent;
    private final List<Node> children;
    private final List<Mesh> meshes;

    public Node(String name, @Nullable Node parent, Matrix4fc transform, List<Mesh> meshes) {
        this.name = name;
        this.transform = transform;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.meshes = meshes;
    }

    public String getName() {
        return name;
    }

    public Matrix4fc getTransform() {
        return transform;
    }

    public @Nullable Node getParent() {
        return parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public List<Mesh> getMeshes() {
        return meshes;
    }

    public Transform getHierarchyTransform() {
        Node root = this;
        Stack<Node> line = new Stack<>();
        line.push(this);
        while ((root = root.getParent()) != null) //Traverse up the tree to the root, recording every node
            line.push(root);

//        Transform transform = new Transform();
//        while (!line.isEmpty()) //Traverse down the tree, applying every transform in succession
//            transform.apply(line.pop().getTransform());

        Matrix4f matrix = new Matrix4f();
        while (!line.isEmpty()) //Traverse down the tree, applying every transform in succession
            matrix.mul(line.pop().getTransform());

        return Transform.fromMatrix4f(matrix);
    }

    @Override
    public void dispose() {
        meshes.forEach(Mesh::dispose);
        children.forEach(Node::dispose);
    }

}

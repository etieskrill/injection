package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.Disposable;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;

public final class Node implements Disposable {

    private final String name;
    private final Matrix4fc transform;
    private final List<Node> children;
    private final List<Mesh> meshes;

    public Node(String name, Matrix4fc transform, List<Mesh> meshes) {
        this.name = name;
        this.transform = transform;
        this.children = new ArrayList<>();
        this.meshes = meshes;
    }

    public String getName() {
        return name;
    }

    public Matrix4fc getTransform() {
        return transform;
    }

    public List<Node> getChildren() {
        return children;
    }

    public List<Mesh> getMeshes() {
        return meshes;
    }

    @Override
    public void dispose() {
        meshes.forEach(Mesh::dispose);
        children.forEach(Node::dispose);
    }

}

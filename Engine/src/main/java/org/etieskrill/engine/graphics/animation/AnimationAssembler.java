package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates all bone-space transforms into model space.
 * <p>
 * May only be used by one {@link Animator} at a time, as the {@code AnimationAssembler} is not thread safe.
 */
public final class AnimationAssembler {

    private final List<Transform> transformPool;
    private int currentTransform;

    public AnimationAssembler(int numNodes) {
        numNodes++; //Root transform
        this.transformPool = new ArrayList<>(numNodes);
        for (int i = 0; i < numNodes; i++) transformPool.add(new Transform());
    }

    public synchronized void transformToModelSpace(List<Transform> boneLocalTransforms, Node node) {
        currentTransform = 1;
        transformPool.forEach(Transform::identity);
        transformToModelSpace(boneLocalTransforms, node, transformPool.getFirst());
    }

    private void transformToModelSpace(List<Transform> boneLocalTransforms, Node node, TransformC transform) {
        Bone bone = node.getBone();
        TransformC localTransform = node.getTransform();

        if (bone != null)
            localTransform = boneLocalTransforms.get(bone.id());

        TransformC nodeTransform = transform.apply(localTransform, transformPool.get(currentTransform++));
        if (bone != null) {
            boneLocalTransforms.get(bone.id())
                    .set(nodeTransform)
                    .apply(bone.offset());
        }

        for (Node child : node.getChildren())
            transformToModelSpace(boneLocalTransforms, child, nodeTransform);
    }

}

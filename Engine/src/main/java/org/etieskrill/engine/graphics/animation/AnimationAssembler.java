package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Node;

import java.util.List;

public final class AnimationAssembler {

    private AnimationAssembler() {
    }

    public static void transformToModelSpace(List<Transform> boneLocalTransforms, Node node, TransformC transform) {
        Bone bone = node.getBone();
        TransformC localTransform = node.getTransform();

        if (bone != null)
            localTransform = boneLocalTransforms.get(bone.id());

        TransformC nodeTransform = transform.apply(localTransform, new Transform());
        if (bone != null) {
            boneLocalTransforms.get(bone.id())
                    .set(nodeTransform)
                    .apply(bone.offset());
        }

        for (Node child : node.getChildren())
            transformToModelSpace(boneLocalTransforms, child, nodeTransform);
    }

}

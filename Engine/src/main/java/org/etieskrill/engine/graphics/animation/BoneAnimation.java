package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Bone;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.List;

public record BoneAnimation(
        Bone bone,
        List<Double> timestamps, //entry 0 in timetamps corresponds to 0 in pos, rot and scaling; etc.
        List<Vector3fc> positions, //TODO probably store in transform
        List<Quaternionfc> rotations,
        List<Vector3fc> scalings,
        Animation.Behaviour preBehaviour, //what happens before first key
        Animation.Behaviour postBehaviour //what happens after last key
) {
}

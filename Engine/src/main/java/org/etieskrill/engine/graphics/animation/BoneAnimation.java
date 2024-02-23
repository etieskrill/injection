package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Bone;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.List;

public record BoneAnimation(
        Bone bone,
        List<Vector3fc> positions,
        List<Double> positionTimes,
        List<Quaternionfc> rotations,
        List<Double> rotationTimes,
        List<Vector3fc> scalings,
        List<Double> scaleTimes,
        Animation.Behaviour preBehaviour, //what happens before first key
        Animation.Behaviour postBehaviour //what happens after last key
) {
}

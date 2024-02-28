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
    @Override
    public String toString() {
        return "BoneAnimation{" +
                "bone=" + bone +
                ", num positions=" + positions.size() +
                ", num positionTimes=" + positionTimes.size() +
                ", num rotations=" + rotations.size() +
                ", num rotationTimes=" + rotationTimes.size() +
                ", num scalings=" + scalings.size() +
                ", num scaleTimes=" + scaleTimes.size() +
                ", preBehaviour=" + preBehaviour +
                ", postBehaviour=" + postBehaviour +
                '}';
    }
}

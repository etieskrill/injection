package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Bone;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.List;

public record BoneAnimation(
        Bone bone,
        List<Vector3fc> positions,
        double[] positionTimes,
        List<Quaternionfc> rotations,
        double[] rotationTimes,
        List<Vector3fc> scalings,
        double[] scaleTimes,
        Animation.Behaviour preBehaviour, //what happens before first key
        Animation.Behaviour postBehaviour //what happens after last key
) {
    @Override
    public String toString() {
        return "BoneAnimation{" +
                "bone=" + bone +
                ", num positions=" + positions.size() +
                ", num positionTimes=" + positionTimes.length +
                ", num rotations=" + rotations.size() +
                ", num rotationTimes=" + rotationTimes.length +
                ", num scalings=" + scalings.size() +
                ", num scaleTimes=" + scaleTimes.length +
                ", preBehaviour=" + preBehaviour +
                ", postBehaviour=" + postBehaviour +
                '}';
    }
}

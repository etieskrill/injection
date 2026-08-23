package org.etieskrill.engine.graphics.animation

import org.etieskrill.engine.graphics.model.Bone
import org.joml.Quaternionfc
import org.joml.Vector3fc

data class BoneAnimation(
    val bone: Bone,
    val positions: List<Vector3fc>,
    val positionTimes: DoubleArray,
    val rotations: List<Quaternionfc>,
    val rotationTimes: DoubleArray,
    val scalings: List<Vector3fc>,
    val scaleTimes: DoubleArray,
    val preBehaviour: AnimationBehaviour, //what happens before first key
    val postBehaviour: AnimationBehaviour //what happens after last key
) {
    override fun toString(): String {
        return "BoneAnimation(bone=$bone, positions=$positions, positionTimes=${
            positionTimes.size
        }, rotations=$rotations, rotationTimes=${rotationTimes.size}, scalings=$scalings, scaleTimes=${
            scaleTimes.size
        }, preBehaviour=$preBehaviour, postBehaviour=$postBehaviour)"
    }
}

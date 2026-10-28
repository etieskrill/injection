package org.etieskrill.engine.graphics.animation

import net.bifreus.kml.Quatc
import net.bifreus.kml.Vec3c
import org.etieskrill.engine.graphics.model.Bone

data class BoneAnimation(
    val bone: Bone,
    val positions: List<Vec3c>,
    val positionTimes: DoubleArray,
    val rotations: List<Quatc>,
    val rotationTimes: DoubleArray,
    val scalings: List<Vec3c>,
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

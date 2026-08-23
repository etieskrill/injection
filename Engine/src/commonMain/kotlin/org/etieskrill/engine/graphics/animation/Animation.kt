package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Bone
import org.lwjgl.assimp.Assimp.*

/**
 * Holds a specific animation for a model. It has an identifying name, a duration, and a speed at which it plays. May
 * optionally have an externally defined global base transform.
 *
 * Confusingly, Assimp sometimes refers to bones as [Nodes][org.lwjgl.assimp.AINodeAnim] in the context of bone animations.
 */
data class Animation(
    val name: String,

    val durationTicks: Int,
    val ticksPerSecond: Double,

    val behaviour: AnimationBehaviour,

    val bones: List<Bone>,
    val boneAnimations: List<BoneAnimation>,
//    val meshChannels: List<MeshAnimation>
) {
    companion object {
        const val MAX_BONE_INFLUENCES = 4
        const val MAX_BONES = 100
    }

    val durationSeconds get() = durationTicks.toDouble() / ticksPerSecond

    internal val bonerMap = HashMap<Bone, BoneAnimation>(boneAnimations.size).apply {
        boneAnimations.forEach { boneAnimation ->
            val bone = bones.find { it == boneAnimation.bone }
                ?: error("Bone ${boneAnimation.bone.name} was not in provided bone set for animation $name")
            put(bone, boneAnimation)
        }
    }

    //TODO add a map from Node/Bone to BoneAnimation so the search for every frame for every node in Animator#_updateBoneMatrices is unnecessary
}

//TODO non-linear interpolation?
enum class AnimationBehaviour(val ai: Int) {
    DEFAULT(aiAnimBehaviour_DEFAULT), //take default transform
    CONSTANT(aiAnimBehaviour_CONSTANT), //no interpolation, use nearest key
    LINEAR(aiAnimBehaviour_LINEAR), //nearest two keys are lerped
    REPEAT(aiAnimBehaviour_REPEAT); //animation wraps keys around

    companion object {
        fun from(aiBehaviour: Int) = entries.find { it.ai == aiBehaviour } ?: DEFAULT
    }
}

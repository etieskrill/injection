package org.etieskrill.engine.graphics.model.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.animation.Animation
import org.etieskrill.engine.graphics.animation.Animation.MAX_BONE_INFLUENCES
import org.etieskrill.engine.graphics.animation.BoneAnimation
import org.etieskrill.engine.graphics.animation.BoneMatcher
import org.etieskrill.engine.graphics.model.Bone
import org.etieskrill.engine.graphics.util.AssimpUtils
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINodeAnim
import org.lwjgl.assimp.AIScene

private val logger = KotlinLogging.logger {}

internal fun loadAnimations(
    scene: AIScene,
    bones: List<Bone>,
    animations: MutableList<Animation>,
    boneMatcher: BoneMatcher
) {
    val animationBuffer = scene.mAnimations() ?: if (scene.mNumAnimations() == 0) return else error("")
    animations += generateSequence { AIAnimation.create(animationBuffer.get()) }
        .take(scene.mNumAnimations())
        .map {
            Animation(
                it.mName().dataString(),
                it.mDuration().toInt(),
                it.mTicksPerSecond(),
                bones,
                loadNodeAnimations(it, bones, boneMatcher),
                null
            )
        }

    logger.debug {
        "Loaded ${animations.size} animation${if (animations.size == 1) "" else "s"} for scene '${
            scene.mName().dataString()
        }': ${animations.map { it.name }}"
    }
}

internal fun loadNodeAnimations(
    animation: AIAnimation,
    bones: List<Bone>,
    boneMatcher: BoneMatcher
): List<BoneAnimation> {
    val channelBuffer = animation.mChannels() ?: if (animation.mNumChannels() != 0) return emptyList() else error("")
    return generateSequence { AINodeAnim.create(channelBuffer.get()) }
        .take(animation.mNumChannels())
        .mapNotNull { node ->
            val name = node.mNodeName().dataString()
            val bone = bones.find { boneMatcher.test(it.name, name) }
            if (bone == null) {
                logger.warn { "Ignored animation node because bone '$name' is not present in rig" }
                return@mapNotNull null
            }

            val (positionTimes, positions) = node.mPositionKeys()!!
                .take(node.mNumPositionKeys())
                .map { it.mTime() to it.mValue().run { Vector3f(x(), y(), z()) } }
                .unzip()
            val (rotationTimes, rotations) = node.mRotationKeys()!!
                .take(node.mNumRotationKeys())
                .map { it.mTime() to it.mValue().run { Quaternionf(x(), y(), z(), w()) } }
                .unzip()
            val (scaleTimes, scalings) = node.mScalingKeys()!!
                .take(node.mNumScalingKeys())
                .map { it.mTime() to it.mValue().run { Vector3f(x(), y(), z()) } }
                .unzip()

            BoneAnimation(
                bone,
                positions, positionTimes.toDoubleArray(),
                rotations, rotationTimes.toDoubleArray(),
                scalings, scaleTimes.toDoubleArray(),
                Animation.Behaviour.from(node.mPreState()),
                Animation.Behaviour.from(node.mPostState())
            )
        }.toList()
}

data class VertexBoneWeights(
    val bones: Vector4i = Vector4i(-1),
    val boneWeights: Vector4f = Vector4f(0f)
)

internal fun getBones(mesh: AIMesh): Pair<List<Bone>?, List<VertexBoneWeights>?> {
    val boneBuffer = mesh.mBones() ?: if (mesh.mNumBones() == 0) return null to null else error("")

    var totalNumVertexWeights = 0
    var boneId = 0

    val vertexBoneWeights = List(mesh.mNumVertices()) { VertexBoneWeights() }
    val maxAffectedLogged = mutableListOf<Int>()

    return (generateSequence { AIBone.create(boneBuffer.get()) }
        .take(mesh.mNumBones())
        .map {
            totalNumVertexWeights += it.mNumWeights()
            loadBoneWeights(boneId, it, vertexBoneWeights, maxAffectedLogged)
            Bone(
                it.mName().dataString(),
                boneId++,
                Transform(AssimpUtils.fromAI(it.mOffsetMatrix()))
            )
        }.toList() to vertexBoneWeights)
        .also { logger.trace { "Loaded $totalNumVertexWeights vertex weight/s for ${it.first.size} bone/s: ${it.first}" } }
}

internal fun loadBoneWeights(
    boneId: Int,
    aiBone: AIBone,
    weights: List<VertexBoneWeights>,
    maxAffectedLogged: MutableList<Int>
) {
    val weightBuffer = aiBone.mWeights()
    generateSequence { weightBuffer.get() }
        .take(aiBone.mNumWeights())
        .forEach {
            if (it.mWeight() == 0f) return@forEach
            val weight = weights[it.mVertexId()]
            if (weight.bones[3] != -1 && boneId !in maxAffectedLogged) {
                maxAffectedLogged += boneId
                logger.warn { "Vertex with id ${it.mVertexId()} is influenced by more than the maximum of $MAX_BONE_INFLUENCES bones" }
                return@forEach
            }

            for (slot in 0..<MAX_BONE_INFLUENCES) {
                if (weight.bones[slot] == boneId) return@forEach
                weight.bones.setComponent(slot, boneId)
                weight.boneWeights.setComponent(slot, it.mWeight())
                break
            }
        }
}

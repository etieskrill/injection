package org.etieskrill.engine.graphics.model.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import net.bifreus.kml.Quat
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4i
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.animation.Animation
import org.etieskrill.engine.graphics.animation.AnimationBehaviour
import org.etieskrill.engine.graphics.animation.BoneAnimation
import org.etieskrill.engine.graphics.model.Bone
import org.etieskrill.engine.graphics.util.AssimpUtils
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINodeAnim
import org.lwjgl.assimp.AIScene

private val logger = KotlinLogging.logger {}

internal fun loadAnimations(
    scene: AIScene,
    bones: List<Bone>,
    boneMatcher: BoneMatcher = DEFAULT_BONE_MATCHER
): List<Animation> {
    val animationBuffer = scene.mAnimations() ?: if (scene.mNumAnimations() == 0) return listOf() else error("")
    val animations = generateSequence { AIAnimation.create(animationBuffer.get()) }
        .take(scene.mNumAnimations())
        .map {
            Animation(
                it.mName().dataString(),
                it.mDuration().toInt(),
                it.mTicksPerSecond(),
                AnimationBehaviour.REPEAT,
                bones,
                loadNodeAnimations(it, bones, boneMatcher),
            )
        }.toList()

    logger.debug {
        "Loaded ${animations.size} animation${if (animations.size == 1) "" else "s"} for scene '${
            scene.mName().dataString()
        }': ${animations.map { it.name }}"
    }

    return animations
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
            val bone = bones.find { boneMatcher(it.name, name) }
            if (bone == null) {
                logger.warn { "Ignored animation node because bone '$name' is not present in rig" }
                return@mapNotNull null
            }

            val (positionTimes, positions) = node.mPositionKeys()!!
                .take(node.mNumPositionKeys())
                .map { it.mTime() to it.mValue().run { Vec3(x(), y(), z()) } }
                .unzip()
            val (rotationTimes, rotations) = node.mRotationKeys()!!
                .take(node.mNumRotationKeys())
                .map { it.mTime() to it.mValue().run { Quat(x(), y(), z(), w()) } }
                .unzip()
            val (scaleTimes, scalings) = node.mScalingKeys()!!
                .take(node.mNumScalingKeys())
                .map { it.mTime() to it.mValue().run { Vec3(x(), y(), z()) } }
                .unzip()

            BoneAnimation(
                bone,
                positions, positionTimes.toDoubleArray(),
                rotations, rotationTimes.toDoubleArray(),
                scalings, scaleTimes.toDoubleArray(),
                AnimationBehaviour.from(node.mPreState()),
                AnimationBehaviour.from(node.mPostState())
            )
        }.toList()
}

data class VertexBoneWeights(
    val bones: Vec4i = Vec4i(-1),
    val boneWeights: Vec4 = Vec4(0f)
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
                logger.warn { "Vertex with id ${it.mVertexId()} is influenced by more than the maximum of ${Animation.MAX_BONE_INFLUENCES} bones" }
                return@forEach
            }

            for (slot in 0..<Animation.MAX_BONE_INFLUENCES) {
                if (weight.bones[slot] == boneId) return@forEach
                weight.bones[slot] = boneId
                weight.boneWeights[slot] = it.mWeight()
                break
            }
        }
}

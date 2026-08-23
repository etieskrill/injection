package org.etieskrill.engine.graphics.animation;

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.model.Node
import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector3f
import org.joml.Vector3fc

private val logger = KotlinLogging.logger {}

/**
 * The [AnimationProvider] is an instance of an [Animation], which is bound to the skeleton of a specific [Model].
 *
 * Other than the bounds described above, an [AnimationProvider] may be reused across any number of
 * [Animators][Animator], so long as the referenced [Model]'s skeleton is compatible.
 */
class AnimationProvider(
    val animation: Animation,
    val model: Model,
    var playbackSpeed: Float = 1f
) {

    private val transformPool = List<Transform>(model.nodeCount) { Transform() }
    private var currentTransform = 0

    init {
        logger.info { "Loading animation '${animation.name}' for model '${model.name}'" }
        validateBonesInModel(animation, model)
    }

    internal fun getLocalBoneTransforms(
        localBoneTransforms: List<Transform>,
        currentTimeSeconds: Double
    ): List<Transform> {
        //TODO get performance counters going, then
        // - pass uniform arrays with single call
        // - bake bone animations into bones / create a map here or in model
        var currentTicks = currentTimeSeconds * animation.ticksPerSecond * playbackSpeed
        when (animation.behaviour) {
            AnimationBehaviour.REPEAT -> currentTicks %= animation.durationTicks
            else -> error("Unexpected behaviour: ${animation.behaviour}")
        }

        currentTransform = 0
        localBoneTransforms.forEach(Transform::identity)
        updateBoneTransforms(localBoneTransforms, currentTicks, model.rootNode)
        return localBoneTransforms
    }

    private fun updateBoneTransforms(localBoneTransforms: List<Transform>, currentTicks: Double, node: Node) {
        val bone = node.bone
        val localTransform = transformPool[currentTransform++] //Set node transform as default
        localTransform.set(node.transform)

        if (bone != null) { //If node has bone, try to find animation
            val boneAnim = animation.bonerMap[bone]

            if (boneAnim != null) { //If bone is animated, replace node transform
                interpolateVector(currentTicks, boneAnim.positionTimes, boneAnim.positions, localTransform.position)
                interpolateQuaternion(currentTicks, boneAnim.rotationTimes, boneAnim.rotations, localTransform.rotation)
                interpolateVector(currentTicks, boneAnim.scaleTimes, boneAnim.scalings, localTransform.scale)
            }

            localBoneTransforms[bone.id].set(localTransform)
        }

        node.children.forEach {
            updateBoneTransforms(localBoneTransforms, currentTicks, it)
        }
    }

    private fun interpolateVector(
        currentTicks: Double,
        timings: DoubleArray,
        vectors: List<Vector3fc>,
        target: Vector3f
    ) {
        target.set(vectors[0])
        val index = findTimingIndex(currentTicks, timings) ?: return
        val t = (currentTicks - timings[index]) / (timings[index + 1] - timings[index])
        vectors[index].lerp(vectors[index + 1], t.toFloat(), target)
    }

    private fun interpolateQuaternion(
        currentTicks: Double,
        timings: DoubleArray,
        quaternions: List<Quaternionfc>,
        target: Quaternionf
    ) {
        target.set(quaternions[0])
        val index = findTimingIndex(currentTicks, timings) ?: return
        val t = (currentTicks - timings[index]) / (timings[index + 1] - timings[index])
        quaternions[index].slerp(quaternions[index + 1], t.toFloat(), target)
    }

    private fun findTimingIndex(currentTicks: Double, timings: DoubleArray): Int? {
        for (i in 0 until timings.size) {
            if (i < timings.size - 1 && currentTicks in timings[i]..timings[i + 1]) {
                return i
            }
        }
        return null
    }

    private fun validateBonesInModel(animation: Animation, model: Model) {
        val bones = animation.boneAnimations.map { it.bone }

        check(bones.all { it in model.bones }) { "Animation contains bones which are not present in the model" }

        if (model.bones.any { it !in bones }) {
            logger.warn { "Animation does not contain animation data for bones in model: ${model.bones.filter { it !in model.bones }}" }
        }
    }

}

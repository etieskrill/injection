package org.etieskrill.engine.graphics.animation

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.TransformC
import org.etieskrill.engine.graphics.model.Node

/**
 * Translates all bone-space transforms into model space.
 * <p>
 * May only be used by one {@link Animator} at a time, as the {@code AnimationAssembler} is not thread safe.
 */
class AnimationAssembler(numNodes: Int) {

    private val transformPool = MutableList(numNodes + 1) { Transform() } // + 1 for root transform
    private var currentTransform: Int = 0
    private val mutex = Mutex()

    fun transformToModelSpace(boneLocalTransforms: List<Transform>, node: Node) = runBlocking { //TODO dis is probs shit
        mutex.withLock {
            currentTransform = 1
            transformPool.forEach(Transform::identity)
            transformToModelSpace(boneLocalTransforms, node, transformPool[0])
        }
    }

    private fun transformToModelSpace(boneLocalTransforms: List<Transform>, node: Node, transform: TransformC) {
        val bone = node.bone
        var localTransform = node.transform

        if (bone != null) {
            localTransform = boneLocalTransforms[bone.id]
        }

        val nodeTransform = transformPool[currentTransform++]
        nodeTransform.set(transform)
        nodeTransform.apply(localTransform)

        if (bone != null) {
            val boneLocalTransform = boneLocalTransforms[bone.id]
            boneLocalTransform.set(nodeTransform)
            boneLocalTransform.apply(bone.offset)
        }

        for (child in node.children) {
            transformToModelSpace(boneLocalTransforms, child, nodeTransform)
        }
    }

}

package org.etieskrill.engine.graphics.animation

import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.model.Node

enum class AnimationBlendMode { ADDITIVE, OVERRIDING }

data class AnimationLayer(
    var blendMode: AnimationBlendMode = AnimationBlendMode.ADDITIVE,
    var filter: NodeFilter? = null,
    var isEnabled: Boolean = true,
    var weight: Float = 1f,
    var playbackSpeed: Float = 1f
)

/**
 * An [AnimationMixer] describes the way a set of animations from an [Animator] are combined.
 *
 * This class has an internal state in order to store the final transforms without creating a new list of potentially
 * hundreds of new single-use objects. Because of this, a given [AnimationMixer] may only be bound to exactly one
 * [Animator].
 */
class AnimationMixer {

    val weights = Weights()

    private val _weights = mutableListOf<Float>()
    internal val animationLayers = mutableListOf<AnimationLayer>()
    private val transforms = List(Animation.MAX_BONES) { Transform() }

    /**
     * [ADDITIVE][AnimationBlendMode.ADDITIVE] animations have their weights normalised across all additive layers,
     * which is useful when mixing several base-layer animations, such as walking in multiple directions, or idling.
     *
     * [OVERRIDING][AnimationBlendMode.OVERRIDING] layers are excluded from said normalisation. This allows
     * transitioning into and out of animations which completely override behaviour of certain parts of a model, such as
     * e.g. a waving animation. However, this can still be overridden by other layers, whether additive or overriding.
     *
     * Negative weights are treated as if they were zero, that is, they are ignored.
     *
     * The first eligible layer is set with a weight of one as a base layer. If there is no eligible animation layer
     * the skeleton's bind pose is assumed.
     */
    internal fun mixAnimations(nodes: List<Node>, providerTransforms: List<List<Transform>>): List<Transform> {
        check(providerTransforms.size == animationLayers.size) { "There must be exactly one layer for each provider" }

        val firstEnabled = animationLayers.indexOfFirst {
            it.isEnabled && (it.blendMode == AnimationBlendMode.OVERRIDING || it.weight > 0f)
        }.takeIf { it != -1 }
            ?: error("At least one animation layer must be enabled") //TODO replace with bind pose and return

        //Set base layer
        transforms.forEachIndexed { i, transform -> transform.set(providerTransforms[firstEnabled][i]) }

        normaliseAdditiveWeights(_weights)

        animationLayers.forEachIndexed { layerIndex, layer ->
            if (!layer.isEnabled || _weights[layerIndex] <= 0) return@forEachIndexed
            val providerTransform = providerTransforms[layerIndex]

            when (layer.blendMode) {
                AnimationBlendMode.ADDITIVE, AnimationBlendMode.OVERRIDING -> {
                    transforms.forEachIndexed { transformIndex, transform ->
                        if (layer.filter != null && !layer.filter!!(nodes[transformIndex])) return@forEachIndexed
                        transform.lerp(providerTransform[transformIndex], _weights[layerIndex])
                    }
                }
            }
        }

        return transforms;
    }

    private fun normaliseAdditiveWeights(weights: MutableList<Float>) {
        weights.clear()

        animationLayers.forEach { layer -> //Filter for and normalise enabled additive layers
            weights += when {
                !layer.isEnabled -> 0f
                layer.blendMode == AnimationBlendMode.ADDITIVE -> layer.weight
                layer.blendMode == AnimationBlendMode.OVERRIDING -> 0f
                else -> error("nopers")
            }
        }

        val sum = weights.sum()
        for (i in weights.indices) {
            weights[i] /= sum
        }

        animationLayers.forEachIndexed { i, layer -> //Re-add enabled overriding layers
            if (layer.isEnabled && layer.blendMode == AnimationBlendMode.OVERRIDING) {
                weights[i] = layer.weight
            }
        }
    }

    inner class Weights {
        operator fun set(index: Int, weight: Float) {
            val additiveLayers = animationLayers.filter { it.blendMode == AnimationBlendMode.ADDITIVE }
            additiveLayers[index].weight = weight
        }
    }

}

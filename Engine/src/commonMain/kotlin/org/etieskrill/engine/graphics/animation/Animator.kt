package org.etieskrill.engine.graphics.animation;

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.TransformC
import org.etieskrill.engine.graphics.model.Model
import org.joml.Matrix4fc
import kotlin.math.max

private val logger = KotlinLogging.logger {}

/**
 * Construct a new animator for [model]. The skeletons of all [Animation]s added to this [Animator] must be compatible
 * with the [Model]'s skeleton.
 *
 * The [Animator] is used to animate a specific, but not necessarily singular, [Model]. It offers a way to group
 * together animations whose skeletons are compatible, as well as an interface for blending between them.
 *
 * The [Animator] acts as the primary control unit in any animation workflow - it also offers the more usable side of
 * the animation api as a whole right now.
 *
 * Every [Model] can have at most one animator influencing it's skinned skeleton.
 */
//TODO in-detail description of animation workflow
class Animator(
    private val model: Model,

    private var isPlaying: Boolean = true,
    private val playbackSpeed: Double = 1.0
) {

    private var currentTimeSeconds = 0.0

    val animationProviders: MutableList<AnimationProvider> = mutableListOf()
    private val providerTransforms: MutableList<List<Transform>> = mutableListOf()

    val transforms = List<TransformC>(Animation.MAX_BONES) { Transform() }

    private val transformsArray: Array<Matrix4fc?> = arrayOfNulls<Matrix4fc>(Animation.MAX_BONES)
    val transformMatricesArray: Array<Matrix4fc>
        get() {
            transforms.forEachIndexed { i, transform -> transformsArray[i] = transform.matrix }
            @Suppress("UNCHECKED_CAST")
            return transformsArray as Array<Matrix4fc>
        }

    val animationMixer: AnimationMixer = AnimationMixer()
    private val animationAssembler: AnimationAssembler = AnimationAssembler(model.nodeCount)

    /**
     * Resets the current time to zero and plays all [Animations][Animation] bound to this [Animator].
     */
    fun play() {
        currentTimeSeconds = 0.0
        isPlaying = true
    }

    /**
     * Sets the current time to [startTimeSeconds] and plays all [Animations][Animation] bound to this
     * [Animator]. Negative time values are clamped to zero.
     *
     * @param startTimeSeconds time to start the animation at
     */
    fun play(startTimeSeconds: Double) {
        currentTimeSeconds = max(0.0, startTimeSeconds)
        isPlaying = true
    }

    /**
     * Stops all [Animations][Animation] bound to this [Animator].
     */
    fun stop() {
        isPlaying = false
    }

    /**
     * Switches the current play-state of all animations bound to this [Animator] to the opposite of what it was at the
     * time of calling.
     *
     * @see Animator.play
     * @see Animator.stop
     */
    fun switchPlaying() {
        if (isPlaying) stop()
        else play()
    }

    /**
     * Updates the current animation time and retrieves all transforms for bound [Animations][Animation] at the new
     * time, combines them using the provided [AnimationMixer], and updates the list of [Transforms][Transform], which
     * can be retrieved with [Animator.transforms].
     *
     * @param delta time since the last animation frame
     */
    fun update(delta: Double) {
        if (!isPlaying) return
        currentTimeSeconds += delta * playbackSpeed

        animationProviders.forEachIndexed { i, provider ->
            provider.getLocalBoneTransforms(providerTransforms[i], currentTimeSeconds)
        }

        val transforms = animationMixer.mixAnimations(model.nodes, providerTransforms)

        //TODO
        // - !get all updated provider transforms
        // - !pass to mixer
        //   !- add (multivariate lerp) or override (set) in layer order
        //   !- apply additive with filter, and override exclusively with filter
        // - !retrieve from mixer
        // - apply post processing (physics sims (cloth, rigid), procedural animation etc.)
        // - !bake into model space
        // - !return final bone matrices

//        if (updates++ % 60 == 0)
//            logger.debug("Playing animation {}, tick {} of {} @ {} ticks/s",
//                    animation.getName(),
//                    String.format("%7.1f", currentTimeSeconds), animation.getDuration(),
//                    String.format("%5.1f", animation.getTicksPerSecond()));

        animationAssembler.transformToModelSpace(transforms, model.nodes[0])

        transforms.forEachIndexed { i, transform -> (this.transforms[i] as Transform).set(transform) }
    }

    /**
     * Adds an animation to this [Animator] using either the provided [layerConfigBlock], or
     * [additive][AnimationBlendMode.ADDITIVE] blending with a weight of `1` and enables it without any [NodeFilter]
     * by default.
     *
     * @param animation the animation
     * @param layerConfigBlock an optional animation layer configuration block
     */
    fun add(animation: Animation, layerConfigBlock: (AnimationLayer) -> Unit = {}) {
        add(animation, AnimationLayer().apply(layerConfigBlock))
    }

    fun addNormalisedGroup(playbackSpeed: Double = 1.0, animConfigBlock: (Animations) -> Unit) {
        val container = Animations().apply(animConfigBlock)

        if (container.animations.isEmpty()) return

        val baseDuration = container.animations[0].durationTicks
        container.animations.forEachIndexed { i, animation ->
            val layer = container.layers[i]
            val duration = animation.durationTicks * layer.playbackSpeed
            layer.playbackSpeed = playbackSpeed.toFloat() * duration / baseDuration
            add(animation, layer)
        }
    }

    private fun add(animation: Animation, layer: AnimationLayer) {
        animationProviders += AnimationProvider(animation, model).apply {
            playbackSpeed = layer.playbackSpeed
        }
        animationMixer.animationLayers += layer

        val providerTransform = MutableList(Animation.MAX_BONES) { Transform() }
        providerTransforms.add(providerTransform)
    }

}

data class Animations(
    internal val animations: MutableList<Animation> = mutableListOf(),
    internal val layers: MutableList<AnimationLayer> = mutableListOf()
) {
    fun add(animation: Animation, layerConfigBlock: (AnimationLayer) -> Unit = {}) {
        animations += animation
        layers += AnimationLayer().apply(layerConfigBlock)
    }
}

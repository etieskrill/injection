package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.model.Model;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.graphics.animation.Animation.MAX_BONES;


/**
 * <p>The {@code Animator} is used to animate a specific, but not necessarily singular, {@link Model}. It offers a way
 * to group together animations whose skeletons are compatible, as well as an interface for blending between them.</p>
 * <p>The {@code Animator} acts as the primary control unit in any animation workflow - it also offers the more usable
 * side of the animation api as a whole right now.</p>
 * <p>Every model which beckons for animation should have at most one animator influencing it's skinned skeleton.</p>
 */
//TODO in-detail description of animation workflow
public class Animator {

    private final List<AnimationProvider> animationProviders;
    private final List<List<Transform>> providerTransforms;
    private final List<TransformC> transforms;

    private final AnimationMixer animationMixer;

    private final Model model;

    private boolean playing;
    private double currentTimeSeconds;
    private double playbackSpeed;

    private static final Logger logger = LoggerFactory.getLogger(Animator.class);

    /**
     * Construct a new animator for a specific {@link Model}. The skeleton of any {@link Animation Animation} added to
     * this {@code Animator} must be compatible with the {@code Model's} skeleton.
     *
     * @param model the model influenced by added animations
     */
    public Animator(@NotNull Model model) {
        this(new ArrayList<>(), new AnimationMixer(), model);
    }

    /**
     * Construct a new animator for a specific {@link Model}. The skeletons of all {@link Animation Animations} passed
     * in this constructor must be compatible with the {@code Model's} skeleton.
     * <br>
     * This is a rather clunky constructor, use {@link Animator#Animator(Model)} instead.
     *
     * @param animationProviders the animations in this group
     * @param animationMixer
     * @param model the model influenced by added animations
     *
     * @see Animator#Animator(Model)
     */
    public Animator(
            @NotNull List<AnimationProvider> animationProviders,
            @NotNull AnimationMixer animationMixer,
            @NotNull Model model
    ) {
        logger.info("Loading {} animation{}: {}", animationProviders.size(), animationProviders.size() == 1 ? "" : "s",
                animationProviders.stream().map(AnimationProvider::getAnimation).map(Animation::getName).toList());

        this.animationProviders = animationProviders;
        this.providerTransforms = new ArrayList<>(animationProviders.size());
        for (int i = 0; i < animationProviders.size(); i++) {
            List<Transform> providerTransform = new ArrayList<>(MAX_BONES);
            for (int j = 0; j < MAX_BONES; j++) providerTransform.add(new Transform());
            providerTransforms.add(providerTransform);
        }

        this.transforms = new ArrayList<>(MAX_BONES);
        for (int i = 0; i < MAX_BONES; i++) transforms.add(new Transform());

        this.animationMixer = animationMixer;

        this.model = model;

        this.playing = false;
        this.currentTimeSeconds = 0;
        this.playbackSpeed = 1;
    }

    /**
     * Resets the current time to zero and plays all {@link Animation Animations} bound to this {@code Animator}.
     */
    public void play() {
        currentTimeSeconds = 0;
        playing = true;
    }

    /**
     * Stops all {@link Animation Animations} bound to this {@code Animator}.
     */
    public void stop() {
        playing = false;
    }

    /**
     * Switches the current play-state of all animations bound to this {@code Animator} to the opposite of what it was
     * at the time of calling.
     *
     * @see Animator#play()
     * @see Animator#stop()
     */
    public void switchPlaying() {
        if (isPlaying()) stop();
        else play();
    }

    /**
     * @return whether bound {@link Animation Animations} are playing
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Updates the current animation time and retrieves all transforms for bound {@link Animation Animations} at the
     * new time, combines them using the provided {@link AnimationMixer}, and updates the list of
     * {@link Transform Transforms}, which can be retrieved with {@link Animator#getTransforms()}.
     *
     * @param delta time since the last animation frame
     */
    public void update(double delta) {
        if (!playing) return;
        currentTimeSeconds += delta * playbackSpeed;

        for (int i = 0; i < animationProviders.size(); i++)
            animationProviders.get(i).getLocalBoneTransforms(providerTransforms.get(i), currentTimeSeconds);

        List<Transform> transforms = animationMixer.mixAnimations(providerTransforms);

        //TODO
        // - !get all updated provider transforms
        // - !pass to mixer
        //   !- add (multivariate lerp) or override (set) in layer order
        //    - apply additive with filter, and override exclusively with filter
        // - !retrieve from mixer
        // - apply post processing (physics sims (cloth, rigid), procedural animation etc.)
        // - !bake into model space
        // - !return final bone matrices

//        if (updates++ % 60 == 0)
//            logger.debug("Playing animation {}, tick {} of {} @ {} ticks/s",
//                    animation.getName(),
//                    String.format("%7.1f", currentTimeSeconds), animation.getDuration(),
//                    String.format("%5.1f", animation.getTicksPerSecond()));

        AnimationAssembler.transformToModelSpace(transforms, model.getNodes().getFirst(), new Transform());

        for (int i = 0; i < this.transforms.size(); i++) {
            ((Transform) this.transforms.get(i)).set(transforms.get(i));
        }
    }

    public Animator add(Animation animation) {
        return add(animation, 0);
    }

    public Animator add(Animation animation, float weight) {
        animationProviders.add(new AnimationProvider(animation, model));
        animationMixer.addAdditiveAnimation(weight);

        List<Transform> providerTransform = new ArrayList<>(MAX_BONES);
        for (int j = 0; j < MAX_BONES; j++) providerTransform.add(new Transform());
        providerTransforms.add(providerTransform);

        return this;
    }

    public List<AnimationProvider> getAnimationProviders() {
        return animationProviders;
    }

    public List<TransformC> getTransforms() {
        return transforms;
    }

    public AnimationMixer getAnimationMixer() {
        return animationMixer;
    }

}

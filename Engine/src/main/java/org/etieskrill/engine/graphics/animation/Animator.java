package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    private Matrix4fc[] transformsArray;

    private final AnimationMixer animationMixer;
    private final AnimationAssembler animationAssembler;

    private final Model model;
    private final List<Node> nodes;

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
        this(new ArrayList<>(), new AnimationMixer(), new AnimationAssembler(model.getNodes().size()), model);
    }

    /**
     * Construct a new animator for a specific {@link Model}. The skeletons of all {@link Animation Animations} passed
     * in this constructor must be compatible with the {@code Model's} skeleton.
     * <br>
     * This constructor is rather clunky, use {@link Animator#Animator(Model)} with the flow methods to add animation
     * instances instead.
     *
     * @param animationProviders the animations in this group
     * @param animationMixer
     * @param model              the model influenced by added animations
     * @see Animator#Animator(Model)
     */
    public Animator(
            @NotNull List<AnimationProvider> animationProviders,
            @NotNull AnimationMixer animationMixer,
            @NotNull AnimationAssembler animationAssembler,
            @NotNull Model model
    ) {
        if (!animationProviders.isEmpty()) {
            logger.info("Loading {} animation{}: {}",
                    animationProviders.size(), animationProviders.size() == 1 ? "" : "s",
                    animationProviders.stream().map(AnimationProvider::getAnimation).map(Animation::getName).toList());
        }

        this.animationProviders = animationProviders;
        this.providerTransforms = new ArrayList<>(animationProviders.size());
        for (int i = 0; i < animationProviders.size(); i++) addNewProviderTransformList();

        this.transforms = new ArrayList<>(MAX_BONES);
        for (int i = 0; i < MAX_BONES; i++) transforms.add(new Transform());

        this.animationMixer = animationMixer;
        this.animationAssembler = animationAssembler;

        this.model = model;
        this.nodes = new ArrayList<>(model.getNodes().size());
        for (int i = 0; i < model.getNodes().size(); i++) this.nodes.add(null);
        model.getNodes().forEach(node -> {
            if (node.getBone() != null) nodes.set(node.getBone().id(), node);
        });

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
     * Sets the current time to {@code startTimeSeconds} and plays all {@link Animation Animations} bound to this
     * {@code Animator}. Negative time values are clamped to zero.
     *
     * @param startTimeSeconds time to start the animation at
     */
    public void play(double startTimeSeconds) {
        currentTimeSeconds = Math.max(0, startTimeSeconds);
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

        List<Transform> transforms = animationMixer.mixAnimations(nodes, providerTransforms);

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

        animationAssembler.transformToModelSpace(transforms, model.getNodes().getFirst());

        for (int i = 0; i < this.transforms.size(); i++) {
            ((Transform) this.transforms.get(i)).set(transforms.get(i));
        }
    }

    /**
     * Adds an animation to this {@code Animator} using {@link AnimationMixer.AnimationBlendMode#ADDITIVE additive}
     * blending with a weight of {@code 1} and enables it without any {@link NodeFilter}.
     *
     * @param animation the animation to add
     * @return the {@code Animator} for chaining
     */
    public Animator add(@NotNull Animation animation) {
        return add(animation, layer -> {});
    }

    /**
     * Adds an animation to this {@code Animator} using the configuration provided in the {@code layer} argument.
     * <p>
     * By default, the layer has {@link AnimationMixer.AnimationBlendMode#ADDITIVE additive} blending with a weight of
     * {@code 1} and disables it, meaning it has no influence, and no {@link NodeFilter} is set.
     *
     * @param animation the animation to add
     * @param layer     the animation layer for configuration
     * @return the {@code Animator} for chaining
     */
    public Animator add(@NotNull Animation animation, @NotNull Consumer<AnimationMixer.AnimationLayer> layer) {
        AnimationMixer.AnimationLayer animationLayer = new AnimationMixer.AnimationLayer();
        layer.accept(animationLayer);

        return add(animation, animationLayer);
    }

    public Animator addNormalisedGroup(@NotNull Consumer<Animations> animations) {
        return addNormalisedGroup(1, animations);
    }

    public Animator addNormalisedGroup(double playbackSpeed, @NotNull Consumer<Animations> animations) {
        Animations container = new Animations();
        animations.accept(container);

        if (container.animations.isEmpty()) return this;

        double baseDuration = container.animations.getFirst().getDurationTicks();
        for (int i = 0; i < container.animations.size(); i++) {
            double duration = container.animations.get(i).getDurationTicks() * container.layers.get(i).getPlaybackSpeed();
            container.layers.get(i).playbackSpeed(playbackSpeed * duration / baseDuration);
            add(container.animations.get(i), container.layers.get(i));
        }

        return this;
    }

    private Animator add(Animation animation, AnimationMixer.AnimationLayer layer) {
        AnimationProvider provider = new AnimationProvider(animation, model);
        provider.setPlaybackSpeed(layer.getPlaybackSpeed());
        animationProviders.add(provider);
        animationMixer.addAnimationLayer(layer);

        addNewProviderTransformList();

        return this;
    }

    public static class Animations {
        private final List<Animation> animations;
        private final List<AnimationMixer.AnimationLayer> layers;

        private Animations() {
            this.animations = new ArrayList<>();
            this.layers = new ArrayList<>();
        }

        public Animations add(@NotNull Animation animation) {
            return add(animation, layer -> {});
        }

        public Animations add(@NotNull Animation animation, @NotNull Consumer<AnimationMixer.AnimationLayer> layer) {
            animations.add(animation);

            AnimationMixer.AnimationLayer animationLayer = new AnimationMixer.AnimationLayer();
            layer.accept(animationLayer);
            layers.add(animationLayer);

            return this;
        }
    }

    public List<AnimationProvider> getAnimationProviders() {
        return animationProviders;
    }

    public List<TransformC> getTransforms() {
        return transforms;
    }

    public List<Matrix4fc> getTransformMatrices() {
        return transforms.stream().map(TransformC::getMatrix).toList();
    }

    public Matrix4fc[] getTransformMatricesArray() {
        if (transformsArray == null) {
            transformsArray = new Matrix4fc[MAX_BONES];
        }
        for (int i = 0; i < transforms.size(); i++) {
            transformsArray[i] = transforms.get(i).getMatrix();
        }
        return transformsArray;
    }

    public AnimationMixer getAnimationMixer() {
        return animationMixer;
    }

    private void addNewProviderTransformList() {
        List<Transform> providerTransform = new ArrayList<>(MAX_BONES);
        for (int j = 0; j < MAX_BONES; j++) providerTransform.add(new Transform());
        providerTransforms.add(providerTransform);
    }

}

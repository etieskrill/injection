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

    public Animator(@NotNull Model model) {
        this(new ArrayList<>(), new AnimationMixer(), model);
    }

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

    public void play() {
        currentTimeSeconds = 0;
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public void switchPlaying() {
        if (isPlaying()) stop();
        else play();
    }

    public boolean isPlaying() {
        return playing;
    }

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
        // - apply post processing ()
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
        animationProviders.add(new AnimationProvider(animation, model));
        animationMixer.addAdditiveAnimation(0);

        List<Transform> providerTransform = new ArrayList<>(MAX_BONES);
        for (int j = 0; j < MAX_BONES; j++) providerTransform.add(new Transform());
        providerTransforms.add(providerTransform);

        return this;
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

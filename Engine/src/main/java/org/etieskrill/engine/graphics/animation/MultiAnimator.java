package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiAnimator extends AbstractAnimator {

    private final List<Animator> animators;
    private final List<Float> weights;

    public MultiAnimator(@NotNull Model model) {
        super(model);

        this.animators = new ArrayList<>();
        this.weights = new ArrayList<>();
    }

    public MultiAnimator(
            @NotNull List<@NotNull Animator> animators,
            @NotNull List<@NotNull Float> weights,
            @NotNull Model model
    ) {
        super(model);

        if (animators.isEmpty())
            throw new IllegalArgumentException("Must specify at least one animator");

        if (animators.stream().anyMatch(animator -> !animator.getModel().equals(model)))
            throw new IllegalArgumentException("Animations must belong to the same model");

        if (animators.size() != weights.size())
            throw new IllegalArgumentException("Mismatch in number of animations and weights");

        this.animators = new ArrayList<>(animators);
        this.weights = new ArrayList<>(weights);
    }

    public static class WeightedAnimator {
        private final Animator animator;
        private float weight;

        public WeightedAnimator(Animator animator, float weight) {
            this.animator = animator;
            this.weight = weight;
        }

        public Animator getAnimator() {
            return animator;
        }

        public float getWeight() {
            return weight;
        }
    }

    @Override
    public void play() {
        animators.forEach(Animator::play);
    }

    @Override
    public void stop() {
        animators.forEach(Animator::stop);
    }

    @Override
    public void update(double delta) {
        animators.forEach(animator -> animator.update(delta));
        MathUtils.normalise(weights);

        //interpolate

        //.33, .33, .33 -> .5 - .5, .66, .33
        //1, 0, 0 -> 1 - 0, 1 - 0
        //.5, .25, .25 -> .66 - .33, .75 - .25
        //.2, .3, .4, .1 -> .4 - .6, .55 - .44, .9 - .1

        boneTransforms.forEach(Transform::identity);
        boolean setInitial = false;

        for (int i = 0; i < animators.size(); i++) {
            if (weights.get(i) == 0) continue;

            List<TransformC> boneTransforms = animators.get(i).getBoneTransforms();

            if (!setInitial) {
                for (int j = 0; j < this.boneTransforms.size(); j++) {
                    this.boneTransforms.get(j).set(boneTransforms.get(j));
                }
                setInitial = true;
                continue;
            }

            if (true) return;
            for (int j = 0; j < this.boneTransforms.size(); j++) {
//                this.boneTransforms.get(j).lerp(boneTransforms.get(j), .5f);
                Transform transform = this.boneTransforms.get(j);
                TransformC other = boneTransforms.get(j);

                final int finalJ = j;
                Bone bone = animators.get(i).getAnimation()
                        .getBoneAnimations().stream()
                        .map(BoneAnimation::bone)
                        .filter(_bone -> _bone.id() == finalJ)
                        .findAny()
                        .orElse(null);

                List<String> filter = List.of(
                        "mixamorig_Spine",
                        "mixamorig_Spine1",
                        "mixamorig_Spine2",
                        "mixamorig_Neck",
                        "mixamorig_Head",
                        "mixamorig_RightShoulder",
                        "mixamorig_RightArm",
                        "mixamorig_RightForeArm",
                        "mixamorig_RightHand"
                );

                if (bone != null && filter.stream().anyMatch(name -> bone.name().contains(name))) {
                    System.out.println("applied to bone: " + bone.name());
                    transform.set(other);
                }
            }
        }
    }

    public List<Animator> getAnimators() {
        return animators;
    }

    public List<Float> getWeights() {
        return weights;
    }

    public void add(Animator animator, float weight) {
        add(animators.size(), animator, weight);
    }

    public void add(int index, Animator animator, float weight) {
        if (animator.getModel() != model)
            throw new IllegalArgumentException("Animator does not belong to current model");
        if (weight < 0)
            throw new IllegalArgumentException("Weight must not be negative");

        animators.add(index, animator);
        weights.add(index, weight);
    }

}

package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiAnimator {

    private final List<WeightedAnimator> animators;

    public MultiAnimator(
            @NotNull List<@NotNull Animator> animators,
            @NotNull List<@NotNull Float> weights
    ) {
        if (animators.isEmpty())
            throw new IllegalArgumentException("Must specify at least one animator");

        Model model = animators.getFirst().getModel();
        if (animators.stream().anyMatch(animator -> !animator.getModel().equals(model)))
            throw new IllegalArgumentException("Animations must belong to the same model");

        if (animators.size() != weights.size())
            throw new IllegalArgumentException("Mismatch in number of animations and weights");
        weights = MathUtils.normalise(weights);

        this.animators = new ArrayList<>(animators.size());
        for (int i = 0; i < animators.size(); i++) {
            this.animators.add(new WeightedAnimator(
                    animators.get(i),
                    weights.get(i)
            ));
        }
    }

    private record WeightedAnimator(
            Animator animator,
            float weight
    ) {
    }

    void play() {
    }

    void stop() {
    }

    void switchPlaying() {
    }
//    boolean isPlaying() {}

    void update(double delta) {
    }

    void setPlaybackSpeed(double speed) {
    }
//    List<Matrix4fc> getBoneMatrices() {}

}

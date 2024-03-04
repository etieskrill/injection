package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.util.MathUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.graphics.animation.Animation.MAX_BONES;

/**
 * An {@code AnimationMixer} describes the way a set of animations from an {@link Animator} are combined.
 * <p>
 * This class has an internal state in order to store the final transforms without creating a new list of potentially
 * hundreds of new single-use objects. Because of this, a given {@code AnimationMixer} may only be bound to exacly one
 * {@link Animator}.
 */
public class AnimationMixer {

    private final List<AnimationLayer> animationLayers;
    private final List<Transform> transforms;

    public AnimationMixer() {
        this.animationLayers = new ArrayList<>();
        this.transforms = new ArrayList<>(MAX_BONES);
        for (int i = 0; i < MAX_BONES; i++) transforms.add(new Transform());
    }

    public AnimationMixer addAdditiveAnimation(float weight) {
        return addAdditiveAnimation(weight, null);
    }

    public AnimationMixer addAdditiveAnimation(float weight, NodeFilter filter) {
        animationLayers.add(new AnimationLayer(AnimationBlendMode.ADDITIVE, weight, filter));
        return this;
    }

    public AnimationMixer addOverridingAnimation(NodeFilter filter) {
        animationLayers.add(new AnimationLayer(AnimationBlendMode.OVERRIDING, filter));
        return this;
    }

    public void setWeights(List<Float> weights) {
        List<AnimationLayer> additiveLayers = animationLayers.stream()
                .filter(layer -> layer.getBlendMode() == AnimationBlendMode.ADDITIVE)
                .toList();

        if (weights.size() != additiveLayers.size())
            throw new IllegalArgumentException("Number of weights does not match number of additive animations");

        for (int i = 0; i < additiveLayers.size(); i++) {
            additiveLayers.get(i).setWeight(weights.get(i));
        }
    }

    List<Transform> mixAnimations(List<Node> nodes, List<List<Transform>> providerTransforms) {
        if (providerTransforms.size() != animationLayers.size())
            throw new IllegalArgumentException("There must be exactly one layer for each provider");

        //Set base layer
        for (int i = 0; i < transforms.size(); i++)
            transforms.get(i).set(providerTransforms.getFirst().get(i));

        //Normalise additive layer weights
        List<Float> weights = new ArrayList<>();
        animationLayers.stream()
                .filter(layer -> layer.getBlendMode() == AnimationBlendMode.ADDITIVE)
                .forEach(layer -> weights.add(layer.getWeight()));
        for (AnimationLayer layer : animationLayers) {
            switch (layer.getBlendMode()) {
                case ADDITIVE -> weights.add(layer.getWeight());
                case OVERRIDING -> weights.add(0f);
            }
        }
        MathUtils.normalise(weights);

        for (int i = 1; i < animationLayers.size(); i++) {
            List<Transform> providerTransform = providerTransforms.get(i);
            AnimationLayer layer = animationLayers.get(i);
            NodeFilter filter = layer.getFilter();
            switch (layer.getBlendMode()) {
                case ADDITIVE -> {
                    for (int j = 0; j < transforms.size(); j++) {
                        if (filter != null && !filter.allows(nodes.get(j))) continue;
                        transforms.get(j).lerp(providerTransform.get(j), weights.get(i));
                    }
                }
                case OVERRIDING -> {
                    for (int j = 0; j < transforms.size(); j++) {
                        if (filter != null && !filter.allows(nodes.get(j))) continue;
                        transforms.get(j).set(providerTransform.get(j));
                    }
                }
            }
        }

        return transforms;
    }

    private static class AnimationLayer {
        private final AnimationBlendMode blendMode;
        private float weight;
        private final @Nullable NodeFilter filter;

        public AnimationLayer(AnimationBlendMode blendMode) {
            this(blendMode, 0f);
        }

        public AnimationLayer(AnimationBlendMode blendMode, float weight) {
            this(blendMode, weight, null);
        }

        public AnimationLayer(AnimationBlendMode blendMode,  @Nullable NodeFilter filter) {
            this(blendMode, 0f, filter);
        }

        public AnimationLayer(AnimationBlendMode blendMode, float weight, @Nullable NodeFilter filter) {
            this.blendMode = blendMode;
            this.weight = weight;
            this.filter = filter;
        }

        public AnimationBlendMode getBlendMode() {
            return blendMode;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public @Nullable NodeFilter getFilter() {
            return filter;
        }
    }

    private enum AnimationBlendMode {
        ADDITIVE,
        OVERRIDING
    }

}

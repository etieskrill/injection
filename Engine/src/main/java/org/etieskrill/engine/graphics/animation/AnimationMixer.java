package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.util.MathUtils;
import org.jetbrains.annotations.NotNull;
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
    private final List<Float> weights;

    public AnimationMixer() {
        this.animationLayers = new ArrayList<>();
        this.transforms = new ArrayList<>(MAX_BONES);
        for (int i = 0; i < MAX_BONES; i++) transforms.add(new Transform());
        this.weights = new ArrayList<>();
    }

    public AnimationMixer addAnimationLayer(AnimationLayer animationLayer) {
        animationLayers.add(animationLayer);
        return this;
    }

    public boolean isEnabled(int index) {
        return animationLayers.get(index).isEnabled();
    }

    public void setEnabled(int index, boolean enabled) {
        animationLayers.get(index).setEnabled(enabled);
    }

    public void setWeight(int index, float weight) {
        animationLayers.get(index).setWeight(weight);
    }

    public void setWeights(List<Float> weights) {
        List<AnimationLayer> additiveLayers = getAdditiveLayers();

        if (weights.size() != additiveLayers.size())
            throw new IllegalArgumentException("Number of weights does not match number of additive animations");

        for (int i = 0; i < additiveLayers.size(); i++) {
            additiveLayers.get(i).setWeight(weights.get(i));
        }
    }

    /**
     * {@link AnimationBlendMode#ADDITIVE ADDITIVE} animations have their weights normalised across all additive layers,
     * which is useful when mixing several base-layer animations, such as walking in multiple directions, or idling.
     * <p>
     * {@link AnimationBlendMode#OVERRIDING OVERRIDING} layers are excluded from said normalisation. This allows
     * transitioning into and out of animations which completely override behaviour of certain parts of a model, such as
     * e.g. a waving animation. However, this can still be overridden by other layers, whether additive or overriding.
     * <p>
     * Negative weights are treated as if they were zero, that is, they are ignored.
     * <p>
     * The first eligible layer is set with a weight of one as a base layer. If there is no eligible animation layer
     * the skeleton's bind pose is assumed.
     */
    List<Transform> mixAnimations(List<Node> nodes, List<List<Transform>> providerTransforms) {
        if (providerTransforms.size() != animationLayers.size())
            throw new IllegalArgumentException("There must be exactly one layer for each provider");

        int firstEnabled = getFirstEnabled();

        if (firstEnabled == -1)
            throw new IllegalStateException("At least one animation layer must be enabled"); //TODO replace with bind pose and return

        //Set base layer
        for (int i = 0; i < transforms.size(); i++)
            transforms.get(i).set(providerTransforms.get(firstEnabled).get(i));

        normaliseAdditiveWeights(weights);

        for (int i = firstEnabled + 1; i < animationLayers.size(); i++) {
            List<Transform> providerTransform = providerTransforms.get(i);
            AnimationLayer layer = animationLayers.get(i);
            if (!layer.isEnabled() || weights.get(i) <= 0) continue;

            NodeFilter filter = layer.getFilter();
            switch (layer.getBlendMode()) {
                case ADDITIVE, OVERRIDING -> {
                    for (int j = 0; j < transforms.size(); j++) {
                        if (filter != null && !filter.allows(nodes.get(j))) continue;
                        transforms.get(j).lerp(providerTransform.get(j), weights.get(i));
                    }
                }
            }
        }

        return transforms;
    }

    private int getFirstEnabled() {
        int firstEnabled = -1;
        for (int i = 0; i < animationLayers.size(); i++) {
            if (animationLayers.get(i).isEnabled() &&
                    (animationLayers.get(i).getBlendMode() == AnimationBlendMode.OVERRIDING
                            || animationLayers.get(i).getWeight() > 0)) {
                firstEnabled = i;
                break;
            }
        }

        return firstEnabled;
    }

    private void normaliseAdditiveWeights(List<Float> weights) {
        weights.clear();
        for (AnimationLayer layer : animationLayers) { //Filter for enabled additive layers
            if (!layer.isEnabled()) {
                weights.add(0f);
                continue;
            }
            switch (layer.getBlendMode()) {
                case ADDITIVE -> weights.add(layer.getWeight());
                case OVERRIDING -> weights.add(0f);
            }
        }

        MathUtils.normalise(weights);

        for (int i = 0; i < weights.size(); i++) { //Re-add enabled overriding layers
            AnimationLayer layer = animationLayers.get(i);
            if (layer.isEnabled() && layer.getBlendMode() == AnimationBlendMode.OVERRIDING) {
                weights.set(i, layer.getWeight());
            }
        }
    }

    public static class AnimationLayer {
        private @NotNull AnimationBlendMode blendMode;
        private @Nullable NodeFilter filter;
        private boolean enabled;
        private float weight;
        private double playbackSpeed;

        public AnimationLayer() {
            this(AnimationBlendMode.ADDITIVE, true, 1, null, 1);
        }

        public AnimationLayer(@NotNull AnimationBlendMode blendMode, boolean enabled, float weight, @Nullable NodeFilter filter, double playbackSpeed) {
            this.blendMode = blendMode;
            this.filter = filter;

            this.enabled = enabled;
            this.weight = weight;

            this.playbackSpeed = playbackSpeed;
        }

        public @NotNull AnimationBlendMode getBlendMode() {
            return blendMode;
        }

        public void setBlendMode(@NotNull AnimationBlendMode blendMode) {
            this.blendMode = blendMode;
        }

        public AnimationLayer blendMode(@NotNull AnimationBlendMode blendMode) {
            setBlendMode(blendMode);
            return this;
        }

        public @Nullable NodeFilter getFilter() {
            return filter;
        }

        public void setFilter(@Nullable NodeFilter filter) {
            this.filter = filter;
        }

        public AnimationLayer filter(@Nullable NodeFilter filter) {
            setFilter(filter);
            return this;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public AnimationLayer weight(float weight) {
            setWeight(weight);
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public AnimationLayer enabled(boolean enabled) {
            setEnabled(enabled);
            return this;
        }

        public double getPlaybackSpeed() {
            return playbackSpeed;
        }

        public void setPlaybackSpeed(double playbackSpeed) {
            this.playbackSpeed = playbackSpeed;
        }

        public AnimationLayer playbackSpeed(double playbackSpeed) {
            this.playbackSpeed = playbackSpeed;
            return this;
        }
    }

    public enum AnimationBlendMode {ADDITIVE, OVERRIDING}

    private List<AnimationLayer> getAdditiveLayers() {
        return animationLayers.stream()
                .filter(layer -> layer.getBlendMode() == AnimationBlendMode.ADDITIVE)
                .toList();
    }

}

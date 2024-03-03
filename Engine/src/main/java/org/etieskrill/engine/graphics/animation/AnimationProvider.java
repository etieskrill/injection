package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AnimationProvider {

    private final Animation animation;
    private final Node rootNode;

    private static final Logger logger = LoggerFactory.getLogger(AnimationProvider.class);

    public AnimationProvider(@NotNull Animation animation, @NotNull Model model) {
        logger.info("Loading animation '{}' for model '{}'", animation.getName(), model.getName());
        validateBonesInModel(animation, model);

        this.animation = animation;
        this.rootNode = model.getNodes().getFirst();
    }

    public Animation getAnimation() {
        return animation;
    }

    public List<Transform> getLocalBoneTransforms(List<Transform> boneTransforms, double currentTimeSeconds) {
        //TODO get performance counters going, then
        // - pass uniform arrays with single call
        // - bake bone animations into bones / create a map here or in model
        double currentTicks = currentTimeSeconds * animation.getTicksPerSecond();
        switch (animation.getBehaviour()) {
            case REPEAT -> currentTicks %= animation.getDuration();
            default -> throw new IllegalArgumentException("Unexpected behaviour: " + animation.getBehaviour());
        }

        boneTransforms.forEach(Transform::identity);
        updateBoneTransforms(boneTransforms, currentTicks, rootNode);
        return boneTransforms;
    }

    private void updateBoneTransforms(List<Transform> boneTransforms, double currentTicks, Node node) {
        Bone bone = node.getBone();
        Transform localTransform = new Transform(node.getTransform()); //Set node transform as default

        if (bone != null) { //If node has bone, try to find animation
            //Add in mixing thingy here, should override boneAnim, or potentially add two???
            BoneAnimation boneAnim = animation.getBoneAnimations().stream()
                    .filter(_boneAnimation -> _boneAnimation.bone().equals(node.getBone()))
                    .findAny()
                    .orElse(null);

            if (boneAnim != null) { //If bone is animated, replace node transform
                Vector3fc position = interpolate(currentTicks, boneAnim, boneAnim.positionTimes(), boneAnim.positions());
                Quaternionfc rotation = interpolate(currentTicks, boneAnim, boneAnim.rotationTimes(), boneAnim.rotations());
                Vector3fc scaling = interpolate(currentTicks, boneAnim, boneAnim.scaleTimes(), boneAnim.scalings());

                localTransform.set(position, rotation, scaling);
            }

            boneTransforms
                    .get(bone.id())
                    .set(localTransform);
        }

        for (Node child : node.getChildren())
            updateBoneTransforms(boneTransforms, currentTicks, child);
    }

    private <T> @NotNull T interpolate(double currentTicks, BoneAnimation anim, List<Double> timings, List<T> values) {
        int numTimings = timings.size();

        //TODO evaluate and implement animation behaviour handling
//        if (currentTicks < timings.getFirst()
//                && anim.preBehaviour() != Animation.Behaviour.REPEAT) {
//            return switch (anim.preBehaviour()) {
//                case DEFAULT -> null;
//                case CONSTANT, LINEAR -> values.getFirst();
//                default -> throw new IllegalStateException("Unexpected value: " + anim.preBehaviour());
//            };
//        } else if (currentTicks > timings.getLast()
//                && anim.preBehaviour() != Animation.Behaviour.REPEAT) {
//            return switch (anim.preBehaviour()) {
//                case DEFAULT -> null;
//                case CONSTANT, LINEAR -> values.getLast();
//                default -> throw new IllegalStateException("Unexpected value: " + anim.preBehaviour());
//            };
//        }

//        currentTicks %= animation.getDuration(); //all other behaviours were filtered above, so animation must loop -> ticks are normalised

        int index = -1;
        for (int i = 0; i < numTimings; i++) {
            if (i < numTimings - 1
                    && timings.get(i) <= currentTicks
                    && timings.get(i + 1) >= currentTicks) {
                index = i;
                break;
            }
        }

        if (index == -1) {
//            logger.warn("Found no valid keyframe for bone '{}' type '{}'", anim.bone().name(), values.getFirst().getClass().getSimpleName()); //surely there won't ever be an empty list, right?
            return values.getFirst();
        }

        double t = (currentTicks - timings.get(index)) / (timings.get(index + 1) - timings.get(index));
        return lerp((float) t, values.get(index), values.get(index + 1));
    }

    private static <T> T lerp(float t, T value1, T value2) {
        return switch (value1) {
            case Vector3fc vector -> (T) vector.lerp((Vector3fc) value2, t, new Vector3f());
            case Quaternionfc quaternion -> (T) quaternion.slerp((Quaternionfc) value2, t, new Quaternionf());
            default -> throw new IllegalStateException("Unexpected value: " + value1);
        };
    }

    protected static void validateBonesInModel(Animation animation, Model model) { //TODO this should happen only once while loading the data
        List<Bone> bones = animation.getBoneAnimations().stream()
                .map(BoneAnimation::bone)
                .toList();

        if (bones.stream().anyMatch(bone -> !model.getBones().contains(bone))) {
            throw new IllegalArgumentException("Animation contains bones which are not present in the model");
        }

        logger.atTrace().log(() -> {
            List<Bone> nonAnimatedBones = model.getBones().stream()
                    .filter(bone -> !bones.contains(bone))
                    .toList();
            if (!nonAnimatedBones.isEmpty())
                return "Bones contain no animation data: " + nonAnimatedBones;
            else
                return "All bones are animated";
        });
    }

}

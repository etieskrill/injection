package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.component.Transform;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code AnimationProvider} is an instance of an {@link Animation}, which is bound to the skeleton of a specific
 * {@link Model}.
 * <p>
 * Other than the bounds described above, an {@code AnimationProvider} may be reused across any number of
 * {@link Animator Animators}, so long as the referenced {@link Model Model's} skeleton is compatible.
 */
public class AnimationProvider {

    private final Animation animation;
    private final Node rootNode;

    private double playbackSpeed;

    private final List<Transform> transformPool;
    private int currentTransform;

    private static final Logger logger = LoggerFactory.getLogger(AnimationProvider.class);

    public AnimationProvider(@NotNull Animation animation, @NotNull Model model) {
        logger.info("Loading animation '{}' for model '{}'", animation.getName(), model.getName());
        validateBonesInModel(animation, model);

        this.animation = animation;
        this.rootNode = model.getNodes().getFirst();

        this.playbackSpeed = 1;

        this.transformPool = new ArrayList<>(model.getNodes().size());
        for (int i = 0; i < model.getNodes().size(); i++) transformPool.add(new Transform());
    }

    public Animation getAnimation() {
        return animation;
    }

    public double getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    List<Transform> getLocalBoneTransforms(List<Transform> localBoneTransforms, double currentTimeSeconds) {
        //TODO get performance counters going, then
        // - pass uniform arrays with single call
        // - bake bone animations into bones / create a map here or in model
        double currentTicks = currentTimeSeconds * animation.getTicksPerSecond() * playbackSpeed;
        switch (animation.getBehaviour()) {
            case REPEAT -> currentTicks %= animation.getDurationTicks();
            default -> throw new IllegalArgumentException("Unexpected behaviour: " + animation.getBehaviour());
        }

        currentTransform = 0;
        localBoneTransforms.forEach(Transform::identity);
        updateBoneTransforms(localBoneTransforms, currentTicks, rootNode);
        return localBoneTransforms;
    }

    private void updateBoneTransforms(List<Transform> localBoneTransforms, double currentTicks, Node node) {
        Bone bone = node.getBone();
        Transform localTransform = transformPool.get(currentTransform++).set(node.getTransform()); //Set node transform as default

        if (bone != null) { //If node has bone, try to find animation
            BoneAnimation boneAnim = animation.getBoneAnimation(bone);

            if (boneAnim != null) { //If bone is animated, replace node transform
                interpolateVector(currentTicks, boneAnim.positionTimes(), boneAnim.positions(), localTransform.getPosition());
                interpolateQuaternion(currentTicks, boneAnim.rotationTimes(), boneAnim.rotations(), localTransform.getRotation());
                interpolateVector(currentTicks, boneAnim.scaleTimes(), boneAnim.scalings(), localTransform.getScale());
            }

            localBoneTransforms
                    .get(bone.id())
                    .set(localTransform);
        }

        for (int i = 0; i < node.getChildren().size(); i++)
            updateBoneTransforms(localBoneTransforms, currentTicks, node.getChildren().get(i));
    }

    private void interpolateVector(double currentTicks, double[] timings, List<Vector3fc> vectors, Vector3f target) {
        int numTimings = timings.length;

        int index = -1;
        for (int i = 0; i < numTimings; i++) {
            if (i < numTimings - 1
                    && timings[i] <= currentTicks
                    && timings[i + 1] >= currentTicks) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            target.set(vectors.getFirst());
            return;
        }
        double t = (currentTicks - timings[index]) / (timings[index + 1] - timings[index]);
        vectors.get(index).lerp(vectors.get(index + 1), (float) t, target);
    }

    private void interpolateQuaternion(double currentTicks, double[] timings, List<Quaternionfc> quaternions, Quaternionf target) {
        int numTimings = timings.length;

        int index = -1;
        for (int i = 0; i < numTimings; i++) {
            if (i < numTimings - 1
                    && timings[i] <= currentTicks
                    && timings[i + 1] >= currentTicks) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            target.set(quaternions.getFirst());
            return;
        }
        double t = (currentTicks - timings[index]) / (timings[index + 1] - timings[index]);
        quaternions.get(index).slerp(quaternions.get(index + 1), (float) t, target);
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

        if (index == -1) { //TODO this should never happen once behaviours are implemented - remove once it is so
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

    private static void validateBonesInModel(Animation animation, Model model) {
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

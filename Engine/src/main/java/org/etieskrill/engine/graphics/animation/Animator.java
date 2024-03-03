package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
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


public class Animator extends AbstractAnimator {

    private final Animation animation;
    private Animation secondAnimation;

    private double currentTicks;

    private static final Logger logger = LoggerFactory.getLogger(Animator.class);

    public Animator(@NotNull Animation animation, @NotNull Model model) {
        super(model);

        logger.info("Loading animation '{}' for model '{}'", animation.getName(), model.getName());
        validateBonesInModel(animation, model);

        this.animation = animation;

        updateBoneMatrices();
    }

    public void play() {
        currentTicks = 0;
        playing = true;
    }

    @Override
    public void stop() {
        playing = false;
    }

    private int updates = 0;
    @Override
    public void update(double delta) {
        if (!playing) return;
        currentTicks += animation.getTicksPerSecond() * delta * playbackSpeed;
        currentTicks %= animation.getDuration();
        updateBoneMatrices();
        if (updates++ % 60 == 0)
            logger.debug("Playing animation {}, tick {} of {} @ {} ticks/s",
                    animation.getName(),
                    String.format("%7.1f", currentTicks), animation.getDuration(),
                    String.format("%5.1f", animation.getTicksPerSecond()));
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setSecondAnimation(Animation secondAnimation) {
        this.secondAnimation = secondAnimation;
    }

    private void updateBoneMatrices() {
        //TODO get performance counters going, then
        // - set matrices instead of replacing entire list
        // - pass uniform arrays with single call (if possible)
        // - bake bone animations into bones / create a map here or in model
        boneTransforms.forEach(Transform::identity); //Technically not necessary, but helps identify if node transform does not work
        Node rootNode = model.getNodes().getFirst();
        _updateBoneMatrices(rootNode, animation.getBaseTransform());
    }

    private void _updateBoneMatrices(Node node, TransformC transform) {
        Bone bone = node.getBone();
        Transform localTransform = new Transform(node.getTransform()); //Set node transform as default

        if (bone != null) { //If node has bone, try to find animation
            //Add in mixing thingy here, should override boneAnim, or potentially add two???
            BoneAnimation boneAnim = animation.getBoneAnimations().stream()
                    .filter(_boneAnimation -> _boneAnimation.bone().equals(node.getBone()))
                    .findAny()
                    .orElse(null);

            BoneAnimation boneAnim2 = null;
            if (secondAnimation != null) {
                boneAnim2 = secondAnimation.getBoneAnimations().stream()
                        .filter(_boneAnimation -> _boneAnimation.bone().equals(node.getBone()))
                        .findAny()
                        .orElse(null);
            }

            if (boneAnim != null) { //If bone is animated, replace node transform
                Vector3fc position = interpolate(boneAnim, boneAnim.positionTimes(), boneAnim.positions());
                Quaternionfc rotation = interpolate(boneAnim, boneAnim.rotationTimes(), boneAnim.rotations());
                Vector3fc scaling = interpolate(boneAnim, boneAnim.scaleTimes(), boneAnim.scalings());

                localTransform.set(position, rotation, scaling);
            }

            List<String> filter = List.of(
//                    "mixamorig_Spine",
//                    "mixamorig_Spine1",
//                    "mixamorig_Spine2",
//                    "mixamorig_Neck",
//                    "mixamorig_Head",
                    "mixamorig_RightShoulder",
                    "mixamorig_RightArm",
                    "mixamorig_RightForeArm",
                    "mixamorig_RightHand"
            );

            if (boneAnim2 != null/* && filter.stream().anyMatch(name -> bone.name().contains(name))*/) {
                Vector3f position = (Vector3f) interpolate(boneAnim2, boneAnim2.positionTimes(), boneAnim2.positions());
                Quaternionf rotation = (Quaternionf) interpolate(boneAnim2, boneAnim2.rotationTimes(), boneAnim2.rotations());
                Vector3f scaling = (Vector3f) interpolate(boneAnim2, boneAnim2.scaleTimes(), boneAnim2.scalings());

                localTransform.lerp(new Transform(position, rotation, scaling), .5f);
            }
        }

        TransformC nodeTransform = transform.apply(localTransform, new Transform());
        if (bone != null) { //Set default or animated transform
            boneTransforms.get(bone.id())
                    .set(nodeTransform)
                    .apply(bone.offset());
        }

        for (Node child : node.getChildren())
            _updateBoneMatrices(child, nodeTransform);
    }

    private <T> @NotNull T interpolate(BoneAnimation anim, List<Double> timings, List<T> values) {
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

}

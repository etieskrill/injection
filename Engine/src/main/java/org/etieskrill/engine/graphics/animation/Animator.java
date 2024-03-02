package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.etieskrill.engine.graphics.animation.Animation.MAX_BONES;


public class Animator {

    private final Animation animation;
    private final Model model;

    private boolean playing;
    private double currentTicks;
    private double playbackSpeed;

    @UnmodifiableView
    private final List<TransformC> boneTransforms;

    //Lazy solution to animations having different global transform from others for same model, which should never
    //really be the case, as this should be resolved while loading the node/animation transforms, or just by not having
    //different conventions and/or file formats within the same goddamn model
    @Deprecated
    private final TransformC baseTransform;

    private static final Logger logger = LoggerFactory.getLogger(Animator.class);

    public Animator(Animation animation, Model model) {
        this(animation, model, new Transform());
    }

    @Deprecated
    public Animator(@NotNull Animation animation, @NotNull Model model, @Nullable TransformC baseTansform) {
        logger.info("Loading animation '{}' for model '{}'", animation.getName(), model.getName());
        validateBonesInModel(animation, model);

        this.animation = animation;
        this.model = model;

        this.playing = false;
        this.playbackSpeed = 1;

        List<Transform> boneTransforms = new ArrayList<>(MAX_BONES);
        for (int i = 0; i < MAX_BONES; i++) boneTransforms.add(new Transform());
        this.boneTransforms = Collections.unmodifiableList(boneTransforms);

        this.baseTransform = baseTansform != null ? baseTansform : new Transform();

        updateBoneMatrices();
    }

    public boolean isPlaying() {
        return playing;
    }

    public void play() {
        this.currentTicks = 0;
        this.playing = true;
    }

    public void stop() {
        this.playing = false;
    }

    public void switchPlaying() {
        if (isPlaying()) stop();
        else play();
    }

    private int updates = 0;
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

    public Model getModel() {
        return model;
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public List<TransformC> getBoneTransforms() {
        return boneTransforms;
    }

    public List<Matrix4fc> getBoneTransformMatrices() {
        return boneTransforms.stream().map(TransformC::getMatrix).toList();
    }

    private void updateBoneMatrices() {
        //TODO get performance counters going, then
        // - set matrices instead of replacing entire list
        // - pass uniform arrays with single call (if possible)
        // - bake bone animations into bones / create a map here or in model
        boneTransforms.forEach(transform -> ((Transform) transform).identity()); //Technically not necessary, but helps identify if node transform does not work
        Node rootNode = model.getNodes().getFirst();
        _updateBoneMatrices(rootNode, baseTransform);
    }

    private void _updateBoneMatrices(Node node, TransformC transform) {
        Bone bone = node.getBone();
        Transform localTransform = new Transform(node.getTransform()); //Set node transform as default

        if (bone != null) { //If node has bone, try to find animation
            BoneAnimation boneAnim = animation.getBoneAnimations().stream()
                    .filter(_boneAnimation -> _boneAnimation.bone().equals(node.getBone()))
                    .findAny()
                    .orElse(null);

            if (boneAnim != null) { //If bone is animated, replace node transform
                Vector3fc position = interpolate(boneAnim, boneAnim.positionTimes(), boneAnim.positions());
                Quaternionfc rotation = interpolate(boneAnim, boneAnim.rotationTimes(), boneAnim.rotations());
                Vector3fc scaling = interpolate(boneAnim, boneAnim.scaleTimes(), boneAnim.scalings());

                localTransform.set(position, rotation, scaling);
            }
        }

        TransformC nodeTransform = transform.apply(localTransform, new Transform());
        if (bone != null) { //Set default or animated transform
            ((Transform) boneTransforms.get(bone.id()))
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

    private static void validateBonesInModel(Animation animation, Model model) { //TODO this should happen only once while loading the data
        List<Bone> bones = animation.boneAnimations().stream()
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

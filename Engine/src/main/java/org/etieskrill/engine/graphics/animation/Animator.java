package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.jetbrains.annotations.NotNull;
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

    private final List<Matrix4fc> boneMatrices;
    @UnmodifiableView
    private final List<Matrix4fc> boneMatricesView;

    private static final Logger logger = LoggerFactory.getLogger(Animator.class);

    public Animator(Animation animation, Model model) {
        this.animation = animation;
        this.model = model;

        this.playing = false;
        this.playbackSpeed = 1;

        this.boneMatrices = new ArrayList<>(MAX_BONES);
        this.boneMatricesView = Collections.unmodifiableList(boneMatrices);

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
        if (updates++ % 60 == 0) logger.debug("Playing animation {}, tick {} of {} @ {} ticks/s", animation.getName(), String.format("%7.1f", currentTicks), animation.getDuration(), String.format("%5.1f", animation.getTicksPerSecond()));
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public List<Matrix4fc> getBoneMatrices() {
        return boneMatricesView;
    }

    private void updateBoneMatrices() {
        boneMatrices.clear();
        for (int i = 0; i < MAX_BONES; i++)
            boneMatrices.add(new Matrix4f());
        Node rootNode = model.getNodes().getFirst();
        Matrix4fc inverseGlobalTransform = rootNode.getTransform().invert(new Matrix4f());
        _updateBoneMatrices(rootNode, new Matrix4f(), inverseGlobalTransform);

        if (boneMatrices.size() > MAX_BONES)
            logger.warn("Animation contains more than the maximum of {} bones", MAX_BONES);
    }

    private void _updateBoneMatrices(Node node, Matrix4fc transform, Matrix4fc globalInverseTransform) {
        List<BoneAnimation> boneAnimations = animation.getBoneAnimations();
        BoneAnimation boneAnim = null;
        int boneId = -1;
        for (BoneAnimation boneAnimation : boneAnimations) {
            if (node.getName().equals(boneAnimation.bone().name())) {
                boneAnim = boneAnimation;
                boneId = boneAnim.bone().id();
                break;
            }
        }

        Transform localTransform = Transform.fromMatrix4f(node.getTransform());
        Matrix4fc offset = null;

        if (boneAnim != null) {
            Vector3fc position = interpolate(boneAnim, boneAnim.positionTimes(), boneAnim.positions());
            Quaternionfc rotation = interpolate(boneAnim, boneAnim.rotationTimes(), boneAnim.rotations());
            Vector3fc scaling = interpolate(boneAnim, boneAnim.scaleTimes(), boneAnim.scalings());

//            if (position != null) localTransform.setPosition(position);
//            if (rotation != null) localTransform.setRotation(rotation);
//            if (scaling != null) localTransform.setScale(scaling);

            localTransform.set(position, rotation, scaling);

            offset = boneAnim.bone().offset();
        }

        Matrix4fc nodeTransform = transform.mul(localTransform.toMat(), new Matrix4f());
        if (boneAnim != null)
            boneMatrices.set(boneId, new Matrix4f(globalInverseTransform).mul(nodeTransform).mul(offset));

        for (Node child : node.getChildren())
            _updateBoneMatrices(child, nodeTransform, globalInverseTransform);
    }

    private <T> @NotNull T interpolate(BoneAnimation anim, List<Double> timings, List<T> values) {
        int numTimings = timings.size();

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

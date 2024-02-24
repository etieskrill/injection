package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
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

    int updates = 0;
    public void update(double delta) {
        if (!playing) return;
        currentTicks += animation.getTicksPerSecond() * delta * playbackSpeed;
        updateBoneMatrices();
        if (updates++ % 60 == 0) System.out.println(currentTicks + " " + animation.getDuration());
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public List<Matrix4fc> getBoneMatrices() {
        return boneMatricesView;
    }

    //    private Matrix4f first, previous;
    private void updateBoneMatrices() {
        boneMatrices.clear();
        for (int i = 0; i < MAX_BONES; i++)
            boneMatrices.add(new Matrix4f());
        Node rootNode = model.getNodes().getFirst();
//        _updateBoneMatrices(rootNode, rootNode.getTransform());
        _updateBoneMatrices(rootNode, new Matrix4f(), rootNode.getTransform().invert(new Matrix4f()));
        if (boneMatrices.size() > MAX_BONES)
            logger.warn("Animation contains more than the maximum of {} bones", MAX_BONES);

//        for (int i = 0; i < boneMatrices.size(); i++) {
//            System.out.println("node " + i + ":\n" + boneMatrices.get(i));
//        }
//        if (first == null) first = new Matrix4f(boneMatrices.get(22));
//        if (previous == null) previous = new Matrix4f(boneMatrices.get(22));
//        if (!previous.equals(boneMatrices.get(22), 0.0000001f))System.out.println("node " + 22 + ":\n" + boneMatrices.get(22));
//        System.out.println("diff from first: " + !first.equals(boneMatrices.get(22), 0.0000001f) + ", diff from prev: " + !previous.equals(boneMatrices.get(22), 0.0000001f));
//        previous = new Matrix4f(boneMatrices.get(22));
//        System.out.println();
    }

    private void _updateBoneMatrices(Node node, Matrix4fc transform, Matrix4fc globalInverseTransform) {
//        System.out.println("node " + boneMatrices.size() + ":\n" + Arrays.toString(transform.get(new float[16])));
//        if (boneMatrices.size() >= 100) return; //TODO why??

//        Optional<BoneAnimation> boneAnimOpt = animation.getBoneAnimations().stream()
//                .filter(anim -> anim.bone().name().equals(node.getName()))
//                .findAny();

        List<BoneAnimation> boneAnimations = animation.getBoneAnimations();
        BoneAnimation boneAnim = null;
        int boneId = -1;
        for (int i = 0; i < boneAnimations.size(); i++) {
            if (node.getName().equals(boneAnimations.get(i).bone().name())) {
                boneAnim = boneAnimations.get(i);
                boneId = i;
                break;
            }
        }

        Matrix4fc localTransform = node.getTransform();
        Matrix4fc offset = null;

        if (/*boneAnimOpt.isPresent()*/boneAnim != null) {
//            BoneAnimation boneAnim = boneAnimOpt.get();
            Vector3fc position = interpolate(boneAnim, boneAnim.positionTimes(), boneAnim.positions());
            Quaternionfc rotation = interpolate(boneAnim, boneAnim.rotationTimes(), boneAnim.rotations());
            Vector3fc scaling = interpolate(boneAnim, boneAnim.scaleTimes(), boneAnim.scalings());

            if (position == null) position = new Vector3f();
            if (rotation == null) rotation = new Quaternionf();
            if (scaling == null) scaling = new Vector3f(1);

            localTransform = new Matrix4f()
                    .scale(scaling)
                    .rotate(new Quaternionf(rotation))
                    .translate(new Vector3f(position));

            offset = boneAnim.bone().offset();
        }

        Matrix4fc nodeTransform = transform.mul(localTransform, new Matrix4f());
        if (/*boneAnimOpt.isPresent()*/boneAnim != null) {
//            boneMatrices.set(boneId, nodeTransform.mul(offset, new Matrix4f()));
            boneMatrices.set(boneId, globalInverseTransform.mul(nodeTransform, new Matrix4f()).mul(offset));
            if (boneId < 6) System.out.println("replaced matrix " + boneId + " with\n" + boneMatrices.get(boneId));
        }

        for (Node child : node.getChildren())
            _updateBoneMatrices(child, nodeTransform, globalInverseTransform);
    }

    private <T> T interpolate(BoneAnimation anim, List<Double> timings, List<T> values) {
        int numTimings = timings.size();

        if (currentTicks < timings.getFirst()
                && anim.preBehaviour() != Animation.Behaviour.REPEAT) {
            return switch (anim.preBehaviour()) {
                case DEFAULT -> null;
                case CONSTANT, LINEAR -> values.getFirst();
                default -> throw new IllegalStateException("Unexpected value: " + anim.preBehaviour());
            };
        } else if (currentTicks > timings.getLast()
                && anim.preBehaviour() != Animation.Behaviour.REPEAT) {
            return switch (anim.preBehaviour()) {
                case DEFAULT -> null;
                case CONSTANT, LINEAR -> values.getLast();
                default -> throw new IllegalStateException("Unexpected value: " + anim.preBehaviour());
            };
        }

        currentTicks %= animation.getDuration(); //all other behaviours were filtered above, so animation must loop -> ticks are normalised

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
            System.out.println("current ticks: " + currentTicks + ", times: " + timings);
            throw new IllegalStateException("Found no valid keyframe for animation");
        }

        double t = (currentTicks - timings.get(index)) / (timings.get(index + 1) - timings.get(index));
//        return lerp((float) t, values.get(index), values.get(index + 1));
        return values.get(index);
    }

    private static <T> T lerp(float t, T value1, T value2) {
        return switch (value1) {
            case Vector3fc vector -> (T) vector.lerp((Vector3fc) value2, t, new Vector3f());
            case Quaternionfc quaternion -> (T) quaternion.slerp((Quaternionfc) value2, t, new Quaternionf());
            default -> throw new IllegalStateException("Unexpected value: " + value1);
        };
    }

}

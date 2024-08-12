package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.graphics.model.Bone;
import org.lwjgl.assimp.AINodeAnim;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.assimp.Assimp.*;

/**
 * Holds a specific animation for a model. It has an identifying name, a duration, and a speed at which it plays. May
 * optionally have an externally defined global base transform.
 * <p>
 * Confusingly, Assimp sometimes refers to bones as '{@link AINodeAnim Nodes}' in the context of bone animations.
 */
public class Animation {

    public static final int MAX_BONE_INFLUENCES = 4;
    public static final int MAX_BONES = 100;

    private final String name;

    private final int duration;
    private final double ticksPerSecond;

    private final Behaviour behaviour;
    //TODO non-linear interpolation?

    private final List<BoneAnimation> boneAnimations;
    private final List<MeshAnimation> meshChannels;

    //TODO add a map from Node/Bone to BoneAnimation so the search for every frame for every node in Animator#_updateBoneMatrices is unnecessary

    private final Map<Bone, BoneAnimation> bonerMap;

    /**
     * Constructs a new instance of an animation.
     *
     * @param name           an identifying name
     * @param duration       the total duration in ticks
     * @param ticksPerSecond how many frames play every second
     * @param boneAnimations
     * @param meshChannels
     */
    public Animation(String name, int duration, double ticksPerSecond, List<Bone> bones, List<BoneAnimation> boneAnimations, List<MeshAnimation> meshChannels) {
        this.name = name;

        this.duration = duration;
        this.ticksPerSecond = ticksPerSecond;

        this.boneAnimations = boneAnimations;
        this.meshChannels = meshChannels;

        this.behaviour = Behaviour.REPEAT; //TODO insert in constructor
        this.bonerMap = new HashMap<>(boneAnimations.size());
        for (BoneAnimation boneAnimation : boneAnimations) {
            Bone bone = bones.stream().filter(_bone -> boneAnimation.bone().equals(_bone)).findAny().orElse(null);
            bonerMap.put(bone, boneAnimation);
        }
    }

    public enum Behaviour {
        DEFAULT(aiAnimBehaviour_DEFAULT), //take default transform
        CONSTANT(aiAnimBehaviour_CONSTANT), //no interpolation, use nearest key
        LINEAR(aiAnimBehaviour_LINEAR), //nearest two keys are lerped
        REPEAT(aiAnimBehaviour_REPEAT); //animation wraps keys around

        private final int aiAnimBehaviour;

        Behaviour(int aiAnimBehaviour) {
            this.aiAnimBehaviour = aiAnimBehaviour;
        }

        int ai() {
            return aiAnimBehaviour;
        }

        //defaults to DEFAULT
        public static Behaviour from(int aiBehaviour) {
            return Arrays.stream(Behaviour.values())
                    .filter(value -> value.ai() == aiBehaviour)
                    .findAny()
                    .orElse(DEFAULT);
        }
    }

    public String getName() {
        return name;
    }

    public int getDurationTicks() {
        return duration;
    }

    public double getDurationSeconds() {
        return duration / ticksPerSecond;
    }

    public double getTicksPerSecond() {
        return ticksPerSecond;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

    public BoneAnimation getBoneAnimation(Bone bone) {
        return bonerMap.get(bone);
    }

    public List<BoneAnimation> getBoneAnimations() {
        return boneAnimations;
    }

}

package org.etieskrill.engine.graphics.animation;

import org.lwjgl.assimp.AINodeAnim;

import java.util.Arrays;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

/**
 * Holds a specific animation for a model. It has an identifying name, a duration, and a speed at which it plays.
 * <p>
 * Confusingly, bones are called '{@link AINodeAnim Nodes}' in Assimp.
 *
 * @param name           an identifying name
 * @param duration       the total duration in ticks
 * @param ticksPerSecond how many frames play every second
 * @param boneAnimations
 * @param meshChannels
 */
public record Animation(
        String name,
        int duration,
        double ticksPerSecond,
        List<BoneAnimation> boneAnimations,
        List<MeshAnimation> meshChannels
) {

    public static final int MAX_BONE_INFLUENCES = 4;
    public static final int MAX_BONES = 100;

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

    public int getDuration() {
        return duration;
    }

    public double getTicksPerSecond() {
        return ticksPerSecond;
    }

    public List<BoneAnimation> getBoneAnimations() {
        return boneAnimations;
    }

}

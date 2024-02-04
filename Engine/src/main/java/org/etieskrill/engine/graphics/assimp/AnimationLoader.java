package org.etieskrill.engine.graphics.assimp;

import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.etieskrill.engine.graphics.assimp.AnimationLoader.AnimBehaviour.from;
import static org.etieskrill.engine.graphics.assimp.Model.fromAI;
import static org.lwjgl.assimp.Assimp.*;

class AnimationLoader {

    public record Animation(
            String name,
            double duration,
            double ticksPerSecond,
            List<NodeAnimation> channels,
            List<MeshAnimation> meshChannels
    ) {}

    public record NodeAnimation(
            String nodeName,
            List<Double> timestamps, //entry 0 in timetamps corresponds to 0 in pos, rot and scaling; etc.
            List<Vector3fc> positions, //TODO probably store in transform
            List<Quaternionfc> rotations,
            List<Vector3fc> scalings,
            AnimBehaviour preBehaviour, //what happens before first key
            AnimBehaviour postBehaviour //what happens after last key
    ) {}

    public record MeshAnimation() {}

    public enum AnimBehaviour {
        DEFAULT(aiAnimBehaviour_DEFAULT), //take default transform
        CONSTANT(aiAnimBehaviour_CONSTANT), //no interpolation, use nearest key
        LINEAR(aiAnimBehaviour_LINEAR), //nearest two keys are lerped
        REPEAT(aiAnimBehaviour_REPEAT); //animation wraps keys around

        private final int aiAnimBehaviour;

        AnimBehaviour(int aiAnimBehaviour) {
            this.aiAnimBehaviour = aiAnimBehaviour;
        }

        int ai() {
            return aiAnimBehaviour;
        }

        //defaults to DEFAULT
        static AnimBehaviour from(int aiBehaviour) {
            return Arrays.stream(AnimBehaviour.values())
                    .filter(value -> value.ai() == aiBehaviour)
                    .findAny()
                    .orElse(DEFAULT);
        }
    }

    static void loadAnimations(AIScene scene, List<Animation> animations) {
        for (int i = 0; i < scene.mNumAnimations(); i++) {
            AIAnimation aiAnimation = AIAnimation.create(scene.mAnimations().get());
            animations.add(new Animation(
                    aiAnimation.mName().dataString(),
                    aiAnimation.mDuration(),
                    aiAnimation.mTicksPerSecond(),
                    loadNodeAnimations(aiAnimation.mNumChannels(), aiAnimation.mChannels()),
                    null
                    ));
        }
    }

    private static List<NodeAnimation> loadNodeAnimations(int numAnims, PointerBuffer animBuffer) {
        List<NodeAnimation> nodeAnims = new ArrayList<>(numAnims);
        for (int i = 0; i < numAnims; i++) {
            AINodeAnim nodeAnim = AINodeAnim.create(animBuffer.get());

            List<Double> timestamps = new ArrayList<>(nodeAnim.mNumPositionKeys());
            List<Vector3fc> positions = nodeAnim.mPositionKeys().stream().limit(nodeAnim.mNumPositionKeys())
                    .peek(key -> timestamps.add(key.mTime()))
                    .map(AIVectorKey::mValue)
                    .map(vector -> (Vector3fc) new Vector3f(vector.x(), vector.y(), vector.z()))
                    .toList();
            List<Quaternionfc> rotations = nodeAnim.mRotationKeys().stream().limit(nodeAnim.mNumRotationKeys())
                    .map(AIQuatKey::mValue)
                    .map(quat -> (Quaternionfc) new Quaternionf(quat.x(), quat.y(), quat.z(), quat.w()))
                    .toList();
            List<Vector3fc> scalings = nodeAnim.mScalingKeys().stream().limit(nodeAnim.mNumScalingKeys())
                    .map(AIVectorKey::mValue)
                    .map(vector -> (Vector3fc) new Vector3f(vector.x(), vector.y(), vector.z()))
                    .toList();
            nodeAnims.add(new NodeAnimation(
                    nodeAnim.mNodeName().dataString(),
                    timestamps, positions, rotations, scalings,
                    from(nodeAnim.mPreState()), from(nodeAnim.mPostState())
            ));
        }
        return nodeAnims;
    }

    public record Bone(
            String name,
            List<BoneWeight> weights,
            Matrix4fc offset
    ) {}

    public record BoneWeight(
            int vertexId,
            float weight
    ) {}

    static List<Bone> getBones(AIMesh mesh) {
        List<Bone> bones = new ArrayList<>(mesh.mNumBones());
        for (int i = 0; i < mesh.mNumBones(); i++) {
            AIBone aiBone = AIBone.create(mesh.mBones().get());
            bones.add(new Bone(
                    aiBone.mName().dataString(),
                    loadBoneWeights(aiBone.mNumWeights(), aiBone.mWeights()),
                    fromAI(aiBone.mOffsetMatrix())
            ));
        }
        return bones;
    }

    private static List<BoneWeight> loadBoneWeights(int numWeights, AIVertexWeight.Buffer weightBuffer) {
        return weightBuffer
                .stream()
                .limit(numWeights)
                .map(aiWeight -> new BoneWeight(aiWeight.mVertexId(), aiWeight.mWeight()))
                .toList();
    }

}

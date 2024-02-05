package org.etieskrill.engine.graphics.assimp;

import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.etieskrill.engine.graphics.assimp.Model.fromAI;
import static org.lwjgl.assimp.Assimp.*;

class AnimationLoader {

    /**
     * Holds a specific animation for a model. It has an identifying name, a duration, and a speed at which it plays.
     * <p>
     * Confusingly, bones are called '{@link AINodeAnim Nodes}' in Assimp.
     *
     * @param name an identifying name
     * @param duration the total duration
     * @param ticksPerSecond how many frames play every second
     * @param boneAnimations
     * @param meshChannels
     */
    public record Animation(
            String name,
            double duration,
            double ticksPerSecond,
            List<BoneAnimation> boneAnimations,
            List<MeshAnimation> meshChannels
    ) {}

    public record BoneAnimation(
            Bone bone,
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

    static void loadAnimations(AIScene scene, List<Animation> animations, List<Mesh> meshes) {
        for (int i = 0; i < scene.mNumAnimations(); i++) {
            AIAnimation aiAnimation = AIAnimation.create(scene.mAnimations().get());
            animations.add(new Animation(
                    aiAnimation.mName().dataString(),
                    aiAnimation.mDuration(),
                    aiAnimation.mTicksPerSecond(),
                    loadNodeAnimations(aiAnimation.mNumChannels(), aiAnimation.mChannels(), meshes),
                    null
                    ));
        }
    }

    private static List<BoneAnimation> loadNodeAnimations(int numAnims, PointerBuffer animBuffer, List<Mesh> meshes) {
        List<BoneAnimation> boneAnims = new ArrayList<>(numAnims);
        for (int i = 0; i < numAnims; i++) {
            AINodeAnim nodeAnim = AINodeAnim.create(animBuffer.get());

            String name = nodeAnim.mNodeName().dataString();
            //since there should be comparatively few bones in a model, extracting them every time should be fine
            Bone bone = meshes
                    .stream()
                    .map(Mesh::getBones)
                    .flatMap(List::stream)
                    .filter(meshBone -> meshBone.name().equals(name))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to load animation because no bone '" + name + "' was found"));

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
            boneAnims.add(new BoneAnimation(
                    bone,
                    timestamps, positions, rotations, scalings,
                    AnimBehaviour.from(nodeAnim.mPreState()), AnimBehaviour.from(nodeAnim.mPostState())
            ));
        }
        return boneAnims;
    }

    /**
     * A singular bone in a mesh. It has a human-readable name (e.g. 'hip', 'left_thigh'), a set of vertices
     * ({@code weights}) it influences, and an offset transformation, of which - to be honest - I have no fucking clue
     * what it does.
     *
     * @param name an identifying name for the bone
     * @param weights a list of the influenced vertices
     * @param offset some tranformation of infinite hoopla
     */
    public record Bone(
            String name,
            List<BoneWeight> weights,
            Matrix4fc offset
    ) {}

    /**
     * A singular vertex, and how much it is influenced by the bone holding it.
     *
     * @param vertex the influenced vertex
     * @param weight the factor of influence
     */
    public record BoneWeight(
            Vertex vertex,
            float weight
    ) {}

    static List<Bone> getBones(AIMesh mesh, List<Vertex> vertices) {
        List<Bone> bones = new ArrayList<>(mesh.mNumBones());
        for (int i = 0; i < mesh.mNumBones(); i++) {
            AIBone aiBone = AIBone.create(mesh.mBones().get());
            bones.add(new Bone(
                    aiBone.mName().dataString(),
                    loadBoneWeights(aiBone.mNumWeights(), aiBone.mWeights(), vertices),
                    fromAI(aiBone.mOffsetMatrix())
            ));
        }
        return bones;
    }

    private static List<BoneWeight> loadBoneWeights(int numWeights, AIVertexWeight.Buffer weightBuffer, List<Vertex> vertices) {
        return weightBuffer
                .stream()
                .limit(numWeights)
                .map(aiWeight -> new BoneWeight(vertices.get(aiWeight.mVertexId()), aiWeight.mWeight()))
                .toList();
    }

}

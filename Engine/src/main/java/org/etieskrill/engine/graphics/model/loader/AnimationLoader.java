package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.BoneAnimation;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.BoneWeight;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Vertex;
import org.etieskrill.engine.graphics.util.AssimpUtils;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.util.ArrayList;
import java.util.List;

class AnimationLoader {

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
                    Animation.Behaviour.from(nodeAnim.mPreState()), Animation.Behaviour.from(nodeAnim.mPostState())
            ));
        }
        return boneAnims;
    }

    static List<Bone> getBones(AIMesh mesh, List<Vertex> vertices) {
        List<Bone> bones = new ArrayList<>(mesh.mNumBones());
        for (int i = 0; i < mesh.mNumBones(); i++) {
            AIBone aiBone = AIBone.create(mesh.mBones().get());
            bones.add(new Bone(
                    aiBone.mName().dataString(),
                    loadBoneWeights(aiBone.mNumWeights(), aiBone.mWeights(), vertices),
                    AssimpUtils.fromAI(aiBone.mOffsetMatrix())
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

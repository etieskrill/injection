package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.BoneAnimation;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Vertex;
import org.etieskrill.engine.graphics.util.AssimpUtils;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.graphics.animation.Animation.MAX_BONE_INFLUENCES;

class AnimationLoader {

    private static final Logger logger = LoggerFactory.getLogger(AnimationLoader.class);

    static void loadAnimations(AIScene scene, Model.Builder builder) {
        PointerBuffer animationBuffer = scene.mAnimations();
        for (int i = 0; i < scene.mNumAnimations(); i++) {
            AIAnimation aiAnimation = AIAnimation.create(animationBuffer.get());
            builder.getAnimations().add(new Animation(
                    aiAnimation.mName().dataString(),
                    (int) aiAnimation.mDuration(),
                    aiAnimation.mTicksPerSecond(),
                    loadNodeAnimations(aiAnimation.mNumChannels(), aiAnimation.mChannels(), builder.getMeshes()),
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

            List<Double> positionTimes = new ArrayList<>(nodeAnim.mNumPositionKeys());
            List<Vector3fc> positions = nodeAnim.mPositionKeys().stream().limit(nodeAnim.mNumPositionKeys())
                    .peek(key -> positionTimes.add(key.mTime()))
                    .map(AIVectorKey::mValue)
                    .map(vector -> (Vector3fc) new Vector3f(vector.x(), vector.y(), vector.z()))
                    .toList();
            List<Double> rotationTimes = new ArrayList<>(nodeAnim.mNumRotationKeys());
            List<Quaternionfc> rotations = nodeAnim.mRotationKeys().stream().limit(nodeAnim.mNumRotationKeys())
                    .peek(key -> rotationTimes.add(key.mTime()))
                    .map(AIQuatKey::mValue)
                    .map(quat -> (Quaternionfc) new Quaternionf(quat.x(), quat.y(), quat.z(), quat.w()))
                    .toList();
            List<Double> scaleTimes = new ArrayList<>(nodeAnim.mNumScalingKeys());
            List<Vector3fc> scalings = nodeAnim.mScalingKeys().stream().limit(nodeAnim.mNumScalingKeys())
                    .peek(key -> scaleTimes.add(key.mTime()))
                    .map(AIVectorKey::mValue)
                    .map(vector -> (Vector3fc) new Vector3f(vector.x(), vector.y(), vector.z()))
                    .toList();
            boneAnims.add(new BoneAnimation(
                    bone,
                    positions, positionTimes, rotations, rotationTimes, scalings, scaleTimes,
                    Animation.Behaviour.from(nodeAnim.mPreState()), Animation.Behaviour.from(nodeAnim.mPostState())
            ));
        }

        return boneAnims;
    }

    static List<Bone> getBones(AIMesh mesh, List<Vertex.Builder> vertices) {
        List<Bone> bones = new ArrayList<>(mesh.mNumBones());
        int boneId = 0;
        PointerBuffer boneBuffer = mesh.mBones();
        for (int i = 0; i < mesh.mNumBones(); i++) {
            AIBone aiBone = AIBone.create(boneBuffer.get());
            logger.info("Loading bone '{}'", aiBone.mName().dataString());
            loadBoneWeights(i, aiBone.mNumWeights(), aiBone.mWeights(), vertices);
            bones.add(new Bone(
                    aiBone.mName().dataString(),
                    boneId++,
                    AssimpUtils.fromAI(aiBone.mOffsetMatrix())
            ));
        }
        return bones;
    }

    private static void loadBoneWeights(int boneId, int numWeights, AIVertexWeight.Buffer weightBuffer, List<Vertex.Builder> vertices) {
        logger.info("Loading {} vertex weights for bone {}", numWeights, boneId);
        weightBuffer
                .stream()
                .limit(numWeights)
                .forEach(aiWeight -> {
                    Vertex.Builder vertex = vertices.get(aiWeight.mVertexId());
                    boolean wasSet = false;
                    for (int i = 0; i < MAX_BONE_INFLUENCES; i++) {
                        if (vertex.bones().get(i) == boneId) continue;
                        if (vertex.bones().get(i) != -1) continue;
                        vertex.bones().setComponent(i, boneId);
                        vertex.boneWeights().setComponent(i, aiWeight.mWeight());
                        wasSet = true;
                        break;
                    }
                    if (!wasSet)
                        logger.warn("Vertex with id '{}' is influenced by more than the maximum of {} bones", aiWeight.mVertexId(), MAX_BONE_INFLUENCES);
                });
    }

}

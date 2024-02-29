package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.BoneAnimation;
import org.etieskrill.engine.graphics.model.Bone;
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
import java.util.stream.Stream;

import static org.etieskrill.engine.graphics.animation.Animation.MAX_BONE_INFLUENCES;

class AnimationLoader {

    private static final Logger logger = LoggerFactory.getLogger(AnimationLoader.class);

    static void loadAnimations(AIScene scene, List<Bone> bones, List<Animation> animations) {
        PointerBuffer animationBuffer = scene.mAnimations();
        for (int i = 0; i < scene.mNumAnimations(); i++) {
            AIAnimation aiAnimation = AIAnimation.create(animationBuffer.get());
            animations.add(new Animation(
                    aiAnimation.mName().dataString(),
                    (int) aiAnimation.mDuration(),
                    aiAnimation.mTicksPerSecond(),
                    loadNodeAnimations(aiAnimation.mNumChannels(), aiAnimation.mChannels(), bones),
                    null
            ));
        }
        logger.debug("Loaded {} animation{} for scene '{}': {}",
                animations.size(), animations.size() == 1 ? "" : "s",
                scene.mName().dataString(), animations.stream().map(Animation::getName).toList());
    }

    private static List<BoneAnimation> loadNodeAnimations(int numAnims, PointerBuffer animBuffer, List<Bone> bones) {
        List<BoneAnimation> boneAnims = new ArrayList<>(numAnims);
        for (int i = 0; i < numAnims; i++) {
            AINodeAnim nodeAnim = AINodeAnim.create(animBuffer.get());

            String name = nodeAnim.mNodeName().dataString();
            //since there should be comparatively few bones in a model, extracting them every time should be fine
            Bone bone = bones.stream()
                    .filter(meshBone -> {
//                        return meshBone.name().equals(name);
                        //TODO pretty lenient bone name matching for the time being to allow for several file formats - should be undone
                        if (name.equals("mixamorig:HeadTop_End")) return false;
                        return meshBone.name().replace("_", "").replace(":", "")
                                .equals(name.replace("_", "").replace(":", ""));
                    })
                    .findAny()
                    .orElse(null);
//                    .orElseThrow(() -> new IllegalStateException(
//                            "Failed to load animation because no bone '" + name + "' was found"));
            if (bone == null) continue;

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
        if (mesh.mNumBones() == 0)
            return List.of();

        List<Bone> bones = new ArrayList<>(mesh.mNumBones());
        final int[] boneId = {0}, totalNumVertexWeights = {0};
        List<Integer> maxAffectedLogged = new ArrayList<>(mesh.mNumBones()); //ik this is bad practice, but have you considered the following: screw you

        PointerBuffer boneBuffer = mesh.mBones();

        Stream.generate(boneBuffer::get)
                .limit(mesh.mNumBones())
                .map(AIBone::create)
                .peek(aiBone -> {
                    int numVertexWeights = aiBone.mNumWeights();
                    totalNumVertexWeights[0] += numVertexWeights;
                    loadBoneWeights(boneId[0], numVertexWeights, aiBone.mWeights(), vertices, maxAffectedLogged);
                }) //TODO look up when this gets optimised away
                .forEach(aiBone -> bones.add(new Bone(
                        aiBone.mName().dataString(),
                        boneId[0]++,
                        AssimpUtils.fromAI(aiBone.mOffsetMatrix())
                )));
        logger.trace("Loaded {} vertex weight{} for {} bone{}: {}",
                totalNumVertexWeights[0], totalNumVertexWeights[0] == 1 ? "" : "s",
                bones.size(), bones.size() == 1 ? "" : "s", bones);
        return bones;
    }

    private static void loadBoneWeights(int boneId, int numWeights, AIVertexWeight.Buffer weightBuffer, List<Vertex.Builder> vertices, List<Integer> maxAffectedLogged) {
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
                    if (!wasSet && !maxAffectedLogged.contains(boneId)) {
                        maxAffectedLogged.add(boneId);
                        logger.warn("Vertex with id '{}' is influenced by more than the maximum of {} bones", aiWeight.mVertexId(), MAX_BONE_INFLUENCES);
                    }
                });
    }

}

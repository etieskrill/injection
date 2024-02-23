package org.etieskrill.engine.graphics.model;

import org.joml.Matrix4fc;
import org.lwjgl.assimp.AIVertexWeight;

import java.util.List;

/**
 * A singular bone in a mesh. It has a human-readable name (e.g. 'hip', 'left_thigh') and an offset matrix, which
 * describes the inverse of the final node transformation, i.e. from model space to bone space.
 * <p>
 * The vertex weights are resolved and fed into the
 * {@link Vertex vertices} themselves, see the {@link org.etieskrill.engine.graphics.model.loader.AnimationLoader#loadBoneWeights(int, int, AIVertexWeight.Buffer, List) function in AnimationLoader}
 * for more detail.
 *
 * @param name    an identifying name for the bone
 * @param offset  some tranformation of infinite hoopla
 */
public record Bone(
        String name,
        Matrix4fc offset
) {
}

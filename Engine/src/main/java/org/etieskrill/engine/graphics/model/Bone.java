package org.etieskrill.engine.graphics.model;

import org.joml.Matrix4fc;
import org.lwjgl.assimp.AIVertexWeight;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

/**
 * A singular bone in a mesh. It has a human-readable name (e.g. 'hip', 'left_thigh') and an offset matrix, which
 * describes the inverse of the final node transformation, i.e. from model space to bone space.
 * <p>
 * The vertex weights are resolved and fed into the
 * {@link Vertex vertices} themselves, see the {@link org.etieskrill.engine.graphics.model.loader.AnimationLoader#loadBoneWeights(int, int, AIVertexWeight.Buffer, List, List) function in AnimationLoader}
 * for more detail.
 *
 * @param name    an identifying name for the bone
 * @param offset  some tranformation of infinite hoopla
 */
public record Bone(
        String name,
        int id,
        Matrix4fc offset
) {
    @Override
    public String toString() {
        return MessageFormat.format("Bone[name={0}, id={1}]",
                name, id/*, Arrays.toString(offset.get(new float[16]))*/
        ); //TODO mayhaps create a verbal approximator for matrices - a full depiction is not very suitable for logging
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bone bone = (Bone) o;
        return id == bone.id && Objects.equals(name, bone.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}

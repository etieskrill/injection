package org.etieskrill.engine.graphics.model;

import org.joml.Matrix4fc;

import java.util.List;

/**
 * A singular bone in a mesh. It has a human-readable name (e.g. 'hip', 'left_thigh'), a set of vertices
 * ({@code weights}) it influences, and an offset transformation, of which - to be honest - I have no fucking clue
 * what it does.
 *
 * @param name    an identifying name for the bone
 * @param weights a list of the influenced vertices
 * @param offset  some tranformation of infinite hoopla
 */
public record Bone(
        String name,
        List<BoneWeight> weights,
        Matrix4fc offset
) {
}

package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.entity.component.TransformC

/**
 * A singular bone in a mesh. It has a human-readable name (e.g. 'hip', 'left_thigh') and an offset matrix, which
 * describes the inverse of the final node transformation, i.e. from model space to bone space.
 *
 * The vertex weights are resolved and fed into the [vertices][Vertex] themselves, see [AnimationLoader.loadBoneWeights]
 * for more detail.
 *
 * @param name   an identifying name for the bone
 * @param offset some transformation of infinite hoopla
 */
data class Bone(val name: String, val id: Int, val offset: TransformC)

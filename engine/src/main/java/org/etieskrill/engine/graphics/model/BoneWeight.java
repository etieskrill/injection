package org.etieskrill.engine.graphics.model;

/**
 * A singular vertex, and how much it is influenced by the bone holding it.
 *
 * @param vertex the influenced vertex
 * @param weight the factor of influence
 */
public record BoneWeight(
        Vertex vertex,
        float weight
) {
}

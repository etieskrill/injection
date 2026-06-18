package org.etieskrill.engine.graphics.model.loader

import org.etieskrill.engine.graphics.gl.VertexArrayObject
import org.etieskrill.engine.graphics.model.Bone
import org.etieskrill.engine.graphics.model.Material
import org.etieskrill.engine.graphics.model.Mesh
import org.etieskrill.engine.graphics.model.Vertex
import org.joml.primitives.AABBf

fun loadToVAO(
    vertices: List<Vertex>,
    indices: List<Int>,
    material: Material,
    bones: List<Bone>? = null,
    boundingBox: AABBf = AABBf(),
    drawMode: Mesh.DrawMode? = null
): Mesh = VertexArrayObject(Vertex.Companion.Accessor, vertices, indices = indices)
    .also { it.unbind() }
    .run { Mesh(material, bones, this, boundingBox, drawMode ?: Mesh.DrawMode.TRIANGLES) }

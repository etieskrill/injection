package org.etieskrill.engine.graphics.model.loader

import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.BufferType
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.model.Bone
import org.etieskrill.engine.graphics.model.Material
import org.etieskrill.engine.graphics.model.Mesh
import org.etieskrill.engine.graphics.model.MeshDrawMode
import org.etieskrill.engine.graphics.model.Vertex
import org.joml.primitives.AABBf

fun loadToVAO(
    vertices: List<Vertex>,
    indices: List<Int>,
    material: Material,
    bones: List<Bone>? = null,
    boundingBox: AABBf = AABBf(),
    drawMode: MeshDrawMode? = null
): Mesh = Mesh(
    material,
    bones,
    VertexArrayObject(
        Vertex.Companion.Accessor,
        BufferObject(Vertex.Companion.Accessor, vertices.size).apply { setData(vertices) },
        BufferObject(VertexArrayObject.IndexArrayAccessor, indices.size, BufferType.ELEMENT_ARRAY).apply {
            setData(
                indices
            )
        }
    ),
    boundingBox,
    drawMode ?: MeshDrawMode.TRIANGLES
)

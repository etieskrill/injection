package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.joml.primitives.AABBf

class Mesh(
    var material: Material,
    val bones: List<Bone>?,
    val vao: VertexArrayObject<Vertex>,
    val boundingBox: AABBf,
    val drawMode: MeshDrawMode
)

enum class MeshDrawMode {
    POINTS, LINES, LINE_LOOP, LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN, QUADS;
}

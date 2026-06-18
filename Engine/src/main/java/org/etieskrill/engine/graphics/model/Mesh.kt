package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.VertexArrayObject
import org.joml.primitives.AABBf
import org.lwjgl.opengl.GL33C.*

class Mesh(
    val material: Material,
    val bones: List<Bone>?,
    val vao: VertexArrayObject<Vertex>,
    val boundingBox: AABBf,
    val drawMode: DrawMode
) : Disposable {

    enum class DrawMode {
        POINTS, LINES, LINE_LOOP, LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN, QUADS;

        fun gl() = when (this) {
            POINTS -> GL_POINTS
            LINES -> GL_LINES
            LINE_LOOP -> GL_LINE_LOOP
            LINE_STRIP -> GL_LINE_STRIP
            TRIANGLES -> GL_TRIANGLES
            TRIANGLE_STRIP -> GL_TRIANGLE_STRIP
            TRIANGLE_FAN -> GL_TRIANGLE_FAN
            QUADS -> GL_QUADS
        }
    }

    override fun dispose() {
        material.dispose()
        vao.dispose()
    }

}

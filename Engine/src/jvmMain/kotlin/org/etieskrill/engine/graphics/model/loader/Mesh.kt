package org.etieskrill.engine.graphics.model.loader

import org.etieskrill.engine.graphics.model.MeshDrawMode
import org.etieskrill.engine.graphics.model.MeshDrawMode.*
import org.lwjgl.opengl.GL11C.*

val MeshDrawMode.gl
    get() = when (this) {
        POINTS -> GL_POINTS
        LINES -> GL_LINES
        LINE_LOOP -> GL_LINE_LOOP
        LINE_STRIP -> GL_LINE_STRIP
        TRIANGLES -> GL_TRIANGLES
        TRIANGLE_STRIP -> GL_TRIANGLE_STRIP
        TRIANGLE_FAN -> GL_TRIANGLE_FAN
        QUADS -> GL_QUADS
    }

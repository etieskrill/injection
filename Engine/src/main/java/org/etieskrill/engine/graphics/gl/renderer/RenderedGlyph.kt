package org.etieskrill.engine.graphics.gl.renderer

import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.joml.Vector2f

data class RenderedGlyph(
    val size: Vector2f = Vector2f(),
    val position: Vector2f = Vector2f(),
    var textureIndex: Int = -1
) {
    companion object {

    }
}

object RenderedGlyphAccessor : VertexArrayAccessor<RenderedGlyph>() {
    override fun registerFields() {
        addField<Vector2f> { it, buffer -> it.size[buffer] }
        addField<Vector2f> { it, buffer -> it.position[buffer] }
        addField<Int> { it, buffer -> buffer.putInt(it.textureIndex) }
    }
}

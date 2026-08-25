package org.etieskrill.engine.graphics.renderer

import org.etieskrill.engine.graphics.buffer.VertexArrayAccessor
import org.joml.Vector2f

data class RenderedGlyph(
    val size: Vector2f = Vector2f(),
    val position: Vector2f = Vector2f(),
    var textureIndex: Int = -1
)

object RenderedGlyphAccessor : VertexArrayAccessor<RenderedGlyph>() {
    override fun registerFields() {
        addField<Vector2f> { it, buffer -> buffer += it.size }
        addField<Vector2f> { it, buffer -> buffer += it.position }
        addField<Int> { it, buffer -> buffer += it.textureIndex }
    }
}

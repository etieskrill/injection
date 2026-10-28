package org.etieskrill.engine.graphics.renderer

import net.bifreus.kml.Vec2
import org.etieskrill.engine.graphics.buffer.VertexArrayAccessor

data class RenderedGlyph(
    val size: Vec2 = Vec2(),
    val position: Vec2 = Vec2(),
    var textureIndex: Int = -1
)

object RenderedGlyphAccessor : VertexArrayAccessor<RenderedGlyph>() {
    override fun registerFields() {
        addField<Vec2> { it, buffer -> buffer += it.size }
        addField<Vec2> { it, buffer -> buffer += it.position }
        addField<Int> { it, buffer -> buffer += it.textureIndex }
    }
}

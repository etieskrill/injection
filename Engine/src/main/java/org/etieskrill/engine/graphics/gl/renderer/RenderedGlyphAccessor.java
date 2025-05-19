package org.etieskrill.engine.graphics.gl.renderer;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor;
import org.joml.Vector2f;

public class RenderedGlyphAccessor extends VertexArrayAccessor<RenderedGlyph> {
    private static final @Getter(lazy = true) RenderedGlyphAccessor instance = new RenderedGlyphAccessor();

    @Override
    protected void registerFields() {
        addField(Vector2f.class, (renderedGlyph, byteBuffer) -> renderedGlyph.getSize().get(byteBuffer));
        addField(Vector2f.class, (renderedGlyph, byteBuffer) -> renderedGlyph.getPosition().get(byteBuffer));
        addField(Integer.class, (renderedGlyph, byteBuffer) -> byteBuffer.putInt(renderedGlyph.getTextureIndex()));
    }
}

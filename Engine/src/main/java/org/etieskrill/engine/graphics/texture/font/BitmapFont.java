package org.etieskrill.engine.graphics.texture.font;

import org.etieskrill.engine.graphics.texture.Texture2D;

import java.util.Map;

public class BitmapFont implements Font {
    
    private final Map<Character, Glyph> glyphs;
    
    private final int lineHeight;
    private final int minLineHeight;
    
    private final String family;
    private final String style;
    
    public BitmapFont(Map<Character, Glyph> glyphs, int lineHeight, int minLineHeight, String family, String style) {
        this.glyphs = glyphs;
        this.lineHeight = lineHeight;
        this.minLineHeight = minLineHeight;
        this.family = family;
        this.style = style;
    }
    
    @Override
    public Glyph getGlyph(char c) {
        return glyphs.get(c);
    }
    
    @Override
    public Glyph[] getGlyphs(String s) {
        return s.codePoints().mapToObj(c -> getGlyph((char) c)).toArray(Glyph[]::new);
    }
    
    @Override
    public int getLineHeight() {
        return lineHeight;
    }
    
    @Override
    public int getMinLineHeight() {
        return minLineHeight;
    }
    
    public String getFamily() {
        return family;
    }
    
    public String getStyle() {
        return style;
    }
    
    @Override
    public void dispose() {
        glyphs.values().stream().map(Glyph::getTexture).forEach(Texture2D::dispose);
    }
    
}

package org.etieskrill.engine.graphics.texture.font;

import org.etieskrill.engine.Disposable;

public interface Font extends Disposable {
    
    Glyph getGlyph(char c);
    Glyph[] getGlyphs(String s);
    
    int getLineHeight();
    int getMinLineHeight();
    
}

package org.etieskrill.engine.graphics.texture.font;

import org.etieskrill.engine.Disposable;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public interface Font extends Disposable {

    int NUM_CHARS_ASCII = 128;
    Vector2ic INVALID_PIXEL_SIZE = new Vector2i(-1);

    Glyph getGlyph(char c);
    Glyph[] getGlyphs(String s);

    Vector2ic getPixelSize();
    int getLineHeight();
    int getMinLineHeight();

}

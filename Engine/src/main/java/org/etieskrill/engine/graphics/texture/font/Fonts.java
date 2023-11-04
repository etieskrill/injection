package org.etieskrill.engine.graphics.texture.font;

import java.io.IOException;

public final class Fonts {
    
    public static final String DEFAULT_FONT = "agencyb.ttf";
    
    public static Font getFixedSize(String file, int size) {
        //return generated bitmap font
        return null;
    }
    
    public static Font getDefault() {
        return getFontOrDefault(DEFAULT_FONT, 24);
    }
    
    public static Font getDefault(int pixelHeight) {
        return getFontOrDefault(DEFAULT_FONT, pixelHeight);
    }
    
    public static Font getFontOrDefault(String path, int pixelHeight) {
        TrueTypeFont generatorFont;
        try {
            generatorFont = new TrueTypeFont(path);
        } catch (IOException e) {
            try {
                generatorFont = new TrueTypeFont(DEFAULT_FONT);
            } catch (IOException ex) {
                throw new RuntimeException("Internal exception: could not load default font", ex);
            }
        }
        
        BitmapFont font;
        try {
            font = generatorFont.generateBitmapFont(pixelHeight);
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate bitmap font", e);
        }
        
        return font;
    }

    private Fonts() {}
    
}

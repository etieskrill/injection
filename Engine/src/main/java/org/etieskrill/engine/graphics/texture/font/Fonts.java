package org.etieskrill.engine.graphics.texture.font;

import org.etieskrill.engine.util.FileUtils;
import org.etieskrill.engine.util.Loaders;

import java.io.IOException;

public final class Fonts {

    public static final String DEFAULT_FONT = "AGENCYB.TTF";

    public static Font getDefault() {
        return getFontOrDefault(DEFAULT_FONT, 24);
    }
    
    public static Font getDefault(int pixelHeight) {
        return getFontOrDefault(DEFAULT_FONT, pixelHeight);
    }
    
    public static Font getFontOrDefault(String path, int pixelHeight) {
        var file = FileUtils.splitTypeFromPath(path);

        if (!"ttf".equalsIgnoreCase(file.getExtension()))
            throw new IllegalArgumentException("Must be TrueType file, but was " + file.getExtension());

        TrueTypeFont generatorFont = (TrueTypeFont) Loaders.FontLoader.get().load(
                "ttf:%s:%d".formatted(file.getPath().toLowerCase(), pixelHeight), () -> {
                    try {
                        return new TrueTypeFont(DEFAULT_FONT);
                    } catch (IOException e) {
                        try {
                            return new TrueTypeFont(DEFAULT_FONT);
                        } catch (IOException ex) {
                            throw new RuntimeException("Internal exception: could not load default font", ex);
                        }
                    }
                });
        
        return Loaders.FontLoader.get().load(
                "bmp:%s:%d".formatted(file.getPath().toLowerCase(), pixelHeight), () -> {
                    try {
                        return generatorFont.generateBitmapFont(pixelHeight);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to generate bitmap font", e);
                    }
                });
    }

    private Fonts() {}
    
}

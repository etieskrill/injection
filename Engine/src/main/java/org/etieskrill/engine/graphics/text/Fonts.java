package org.etieskrill.engine.graphics.text;

import org.etieskrill.engine.util.EngineFontLoader;
import org.etieskrill.engine.util.FileUtilsKt;

import java.io.IOException;

import static org.etieskrill.engine.config.ResourcePathsKt.FONT_PATH;

public final class Fonts {

    public static final int DEFAULT_FONT_SIZE = 24;
    public static final String DEFAULT_FONT = FONT_PATH + "AGENCYB.TTF";

    public static Font getDefault() {
        return getFontOrDefault(DEFAULT_FONT, DEFAULT_FONT_SIZE);
    }
    
    public static Font getDefault(int pixelHeight) {
        return getFontOrDefault(DEFAULT_FONT, pixelHeight);
    }
    
    public static Font getFontOrDefault(String path, int pixelHeight) {
        if (!"ttf".equalsIgnoreCase(FileUtilsKt.getExtension(path)))
            throw new IllegalArgumentException("Must be TrueType file, but was " + FileUtilsKt.getExtension(path));

        TrueTypeFont generatorFont = (TrueTypeFont) EngineFontLoader.INSTANCE.load(
                "ttf:%s:%d".formatted(FileUtilsKt.getPath(path).toLowerCase(), pixelHeight), () -> {
                    try {
                        return new TrueTypeFont(path);
                    } catch (IOException e) {
                        try {
                            return new TrueTypeFont(DEFAULT_FONT);
                        } catch (IOException ex) {
                            throw new RuntimeException("Internal exception: could not load default font", ex);
                        }
                    }
                });

        return EngineFontLoader.INSTANCE.load(
                "bmp:%s:%d".formatted(FileUtilsKt.getPath(path).toLowerCase(), pixelHeight), () -> {
                    try {
                        return generatorFont.generateBitmapFont(pixelHeight);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to generate bitmap font", e);
                    }
                });
    }

    private Fonts() {}
    
}

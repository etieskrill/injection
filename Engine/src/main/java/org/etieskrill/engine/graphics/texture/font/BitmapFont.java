package org.etieskrill.engine.graphics.texture.font;

import org.etieskrill.engine.graphics.texture.ArrayTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;

import java.util.Map;
import java.util.Objects;

public class BitmapFont implements Font {

    private final Map<Character, Glyph> glyphs;

    private final int lineHeight;
    private final int minLineHeight;
    private final Vector2ic pixelSize;

    private final String family;
    private final String style;

    private final ArrayTexture textures;

    public BitmapFont(Map<Character, Glyph> glyphs,
                      int lineHeight,
                      int minLineHeight,
                      Vector2ic pixelSize,
                      String family,
                      String style,
                      @Nullable ArrayTexture textures
    ) {
        if (textures == null && glyphs.values().stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Glyph atlas must be set if glyphs do not contain own textures");

        this.glyphs = glyphs;
        this.lineHeight = lineHeight;
        this.pixelSize = pixelSize;
        this.minLineHeight = minLineHeight;
        this.family = family;
        this.style = style;
        this.textures = textures;
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

    @Override
    public Vector2ic getPixelSize() {
        return pixelSize;
    }

    public String getFamily() {
        return family;
    }

    public String getStyle() {
        return style;
    }

    public ArrayTexture getTextures() {
        return textures;
    }

    @Override
    public void dispose() {
        glyphs.values().stream()
                .map(Glyph::getTexture)
                .filter(Objects::nonNull)
                .forEach(Texture2D::dispose);
        if (textures != null) textures.dispose();
    }

}

package org.etieskrill.engine.scene.component;

import org.joml.Vector2f;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.etieskrill.engine.graphics.texture.font.Fonts;
import org.etieskrill.engine.graphics.texture.font.Glyph;
import org.etieskrill.engine.input.Key;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class Label extends Node {
    
    private final Font font;
    private String text;
    
    public Label() {
        this("", Fonts.getDefault());
    }
    
    public Label(String text, Font font) {
        this.text = text;
        this.font = Objects.requireNonNull(font);
    }
    
    @Override
    public void format() {
        if (!shouldFormat()) return;

        int width = 0, height = font.getMinLineHeight();
        for (Glyph glyph : font.getGlyphs(text)) {
            width += glyph.getAdvance().x();
            switch (requireNonNullElse(glyph.getCharacter(), (char) 0)) {
                case '\n' -> height += font.getLineHeight();
            }
        }
        
        setSize(new Vector2f(width, height));
    }
    
    @Override
    public void render(Batch batch) {
        if (text != null) batch.render(text, font, getAbsolutePosition());
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        if (!this.text.equals(text)) invalidate();
        this.text = text;
    }

    @Override
    public boolean hit(Key button, int action, double posX, double posY) {
        return false;
    }

}

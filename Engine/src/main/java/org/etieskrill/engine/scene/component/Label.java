package org.etieskrill.engine.scene.component;

import kotlin.NotImplementedError;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.text.Font;
import org.etieskrill.engine.graphics.text.Fonts;
import org.etieskrill.engine.graphics.text.Glyph;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class Label extends Node<Label> {

    private final Font font;
    private String text;

    public Label() {
        this("", Fonts.getDefault());
    }

    public Label(String text) {
        this(text, Fonts.getDefault());
    }

    public Label(String text, Font font) {
        this.text = text;
        this.font = Objects.requireNonNull(font);
    }

    @Override
    public void computeFixedSizes() {
        if (!shouldFormat()) return;

        switch (getScaleMode()) {
            case FIXED -> {
                getFormattedSize().set(getSize());
                setComputedFixedSize(true);
            }
            case CONTENT -> {
                int maxWidth = 0, width = 0, height = font.getMinLineHeight();
                for (Glyph glyph : font.getGlyphs(text)) {
                    width += glyph.getAdvance().x();
                    switch (requireNonNullElse(glyph.getCharacter(), (char) 0)) {
                        case '\n' -> {
                            height += font.getLineHeight();
                            maxWidth = Math.max(maxWidth, width);
                            width = 0;
                        }
                    }
                }
                maxWidth = Math.max(maxWidth, width);

                getFormattedSize().set(maxWidth, height); //TODO figure out or compute actual font line height and add toggle here
                setComputedFixedSize(true);
            }
            case GROW -> throw new NotImplementedError("ScaleMode.GROW for Label");
        }
    }

    @Override
    public void render(@NotNull Batch batch) {
        if (text != null) batch.renderText(text, font, getAbsolutePosition());
    }

    public String getText() {
        return text;
    }

    public Label setText(String text) {
        if (!this.text.equals(text)) invalidate();
        this.text = text;
        return this;
    }

}

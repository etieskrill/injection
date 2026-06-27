package org.etieskrill.engine.graphics;

import org.etieskrill.engine.config.GraphicsContext;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.text.Font;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector2ic;

public interface TextRenderer {

    /**
     * Computes the cursor position relative to the {@code text}'s position given the integer row and column in
     * {@code cursorPosition} for a given {@code font} and text wrapping border {@code size}. Text is not wrapped if
     * {@code size} is {@code null}.
     *
     * @param cursorPosition the cursor's desired row and column in {@code text}
     * @param text           defines the (potentially) irregular text grid
     * @param font           defines the character sizes
     * @param size           if not null, defines the wrapping border
     * @return the relative position at {@code cursorPosition}, or {@code null} if {@code cursorPosition} exceeds
     * {@code text}
     */
    @Nullable Vector2f getAbsoluteCursorPosition(
            Vector2ic cursorPosition,
            String text,
            Font font,
            @Nullable Vector2fc size
    );

    /**
     * @param cursorPosition will be set to the relative cursor position after rendering the text
     */
    void render(
            String chars,
            Font font,
            Vector2fc position,
            ShaderProgram shader,
            Matrix4fc combined,
            @Nullable Vector2f cursorPosition
    );

    /**
     * @param cursorPosition will be set to the relative cursor position after rendering the text
     */
    void render(
            String chars,
            Font font,
            Vector2fc position,
            @Nullable Vector2fc size,
            ShaderProgram shader,
            Matrix4fc combined,
            @Nullable Vector2f cursorPosition
    );

    /**
     * @return graphics context this text renderer is bound to
     */
    GraphicsContext getContext();

}

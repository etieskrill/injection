package org.etieskrill.engine.graphics;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.text.Font;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;

public interface TextRenderer {

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

}

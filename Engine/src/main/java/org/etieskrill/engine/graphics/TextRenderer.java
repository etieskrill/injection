package org.etieskrill.engine.graphics;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.text.Font;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;

public interface TextRenderer {

    void render(String chars, Font font, Vector2fc position, ShaderProgram shader, Matrix4fc combined);

}

package org.etieskrill.engine.graphics;

import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

public class Batch {

    private final Renderer renderer;
    private final TextRenderer textRenderer;

    //TODO declare this as a pure ui rendering batch (or make subclass) and hardcode shaders here, with a colour stack n stuff
    private ShaderProgram shader;
    private ShaderProgram textShader;

    private final Matrix4f combined;

    public Batch(@NotNull GLRenderer renderer) {
        this(renderer, renderer);
    }

    public Batch(@NotNull Renderer renderer, @NotNull TextRenderer textRenderer) {
        this.renderer = renderer;
        this.textRenderer = textRenderer;
        this.shader = Shaders.getTextureShader();
        this.textShader = Shaders.getTextShader();
        this.combined = new Matrix4f().identity();
    }

    private static final Vector4f resetColour = new Vector4f(1);

    public void render(Vector3f position, Vector3f size, Vector4f colour) {
        shader.setUniform("colour", colour, false);
        renderer.renderBox(position, size, shader, combined);
        shader.setUniform("colour", resetColour, false);
    }

    public void render(String text, Font font, Vector2f position) {
        textRenderer.render(text, font, position, textShader, combined);
    }

    public Batch setCombined(Matrix4fc mat) {
        this.combined.set(mat);
        return this;
    }

    public ShaderProgram getShader() {
        return shader;
    }

    public Batch setShader(ShaderProgram shader) {
        this.shader = shader;
        return this;
    }

}

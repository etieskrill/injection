package org.etieskrill.engine.graphics;

import org.joml.*;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.jetbrains.annotations.NotNull;

public class Batch {

    private final Renderer renderer;

    //TODO declare this as a pure ui rendering batch (or make subclass) and hardcode shaders here, with a colour stack n stuff
    private ShaderProgram shader;

    private final Matrix4f combined;

    public Batch(@NotNull Renderer renderer) {
        this.renderer = renderer;
        this.shader = Shaders.getTextureShader();
        this.combined = new Matrix4f().identity();
    }

    private static final Vector4f resetColour = new Vector4f(1);

    public void render(Vector3f position, Vector3f size, Vector4f colour) {
        shader.setUniform("uColour", colour, false);
        renderer.renderBox(position, size, shader, combined);
        shader.setUniform("uColour", resetColour, false);
    }

    public void render(String text, Font font, Vector2f position) {
        renderer.render(text, font, position, shader, combined);
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

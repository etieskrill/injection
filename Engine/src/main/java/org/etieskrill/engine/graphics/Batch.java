package org.etieskrill.engine.graphics;

import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

//TODO since everything apart from like, chars in Strings is immediate-mode, this should be renamed
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

    /**
     * @param position <b>CENTER</b> point of the box
     */
    public void renderCenteredBox(Vector3fc position, Vector3fc size, Vector4fc colour) {
        shader.setUniform("colour", colour, false);
        renderer.renderBox(position, size, shader, combined);
        shader.setUniform("colour", resetColour, false);
    }

    public void renderBox(Vector3fc position, Vector3fc size, Vector4fc colour) {
        shader.setUniform("colour", colour, false);
        var topLeftPosition = new Vector3f(size).div(2).add(position);
        renderer.renderBox(topLeftPosition, size, shader, combined);
        shader.setUniform("colour", resetColour, false);
    }

    public void renderText(String text, Font font, Vector2fc position) {
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

package org.etieskrill.engine.graphics;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader;
import org.etieskrill.engine.graphics.text.Font;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

//TODO since everything apart from like, chars in Strings is immediate-mode, this should be renamed
public class Batch {

    private final Renderer renderer;
    private final TextRenderer textRenderer;

    //TODO declare this as a pure ui rendering batch (or make subclass) and hardcode shaders here, with a colour stack n stuff
    private @Getter ShaderProgram shader;
    private final ShaderProgram textShader;
    private final BlitShader blitShader;

    private final Matrix4f combined;
    private final Vector2ic viewportSize;

    public Batch(@NotNull GLRenderer renderer, @NotNull Vector2ic viewportSize) {
        this(renderer, renderer, viewportSize);
    }

    public Batch(@NotNull Renderer renderer, @NotNull TextRenderer textRenderer, @NotNull Vector2ic viewportSize) {
        this.renderer = renderer;
        this.textRenderer = textRenderer;
        this.shader = Shaders.getTextureShader();
        this.textShader = Shaders.getTextShader();
        this.blitShader = new BlitShader();
        this.combined = new Matrix4f().identity();
        this.viewportSize = viewportSize;
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

    private int dummyVAO = -1;

    private int getDummyVAO() {
        if (dummyVAO == -1)
            dummyVAO = glGenVertexArrays();
        return dummyVAO;
    }

    public void blit(Texture2D texture, Vector2fc position, Vector2fc size, float rotation) {
        blit(texture, position, size, rotation, null);
    }

    public void blit(Texture2D texture, Vector2fc position, Vector2fc size, float rotation, @Nullable Vector4fc colour) {
        blitShader.setSprite(texture);
        blitShader.setPosition(position);
        blitShader.setSize(size);
        blitShader.setRotation(rotation);
        blitShader.setWindowSize(new Vector2f(viewportSize));
        if (colour != null) blitShader.setColour(colour);
        blitShader.start();

        glBindVertexArray(getDummyVAO());
        glDisable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBlendFunc(GL_ONE, GL_ZERO);
    }

    public Batch setCombined(Matrix4fc mat) {
        this.combined.set(mat);
        return this;
    }

    public Batch setShader(ShaderProgram shader) {
        this.shader = shader;
        return this;
    }

}

package org.etieskrill.engine.graphics;

import lombok.Getter;
import org.etieskrill.engine.graphics.animation.UiOutlineShader;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader;
import org.etieskrill.engine.graphics.pipeline.Pipeline;
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline;
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

    private final FrameBuffer frameBuffer;

    private final Renderer renderer;
    private final TextRenderer textRenderer;

    //TODO declare this as a pure ui rendering batch (or make subclass) and hardcode shaders here, with a colour stack n stuff
    private @Getter ShaderProgram shader;
    private final ShaderProgram textShader;
    private final BlitShader blitShader;

    private final Matrix4f combined;
    private final Vector2ic viewportSize;

    private final PostPassPipeline<UiOutlineShader> uiOutlinePipeline;

    public Batch(@NotNull FrameBuffer frameBuffer, @NotNull GLRenderer renderer) {
        this(frameBuffer, renderer, frameBuffer.getSize());
    }

    public Batch(@NotNull FrameBuffer frameBuffer, @NotNull Renderer renderer, @NotNull TextRenderer textRenderer) {
        this(frameBuffer, renderer, textRenderer, frameBuffer.getSize());
    }

    @Deprecated
    public Batch(@NotNull FrameBuffer frameBuffer, @NotNull GLRenderer renderer, @NotNull Vector2ic viewportSize) {
        this(frameBuffer, renderer, renderer, viewportSize);
    }

    @Deprecated
    public Batch(FrameBuffer frameBuffer, @NotNull Renderer renderer, @NotNull TextRenderer textRenderer, @NotNull Vector2ic viewportSize) {
        this.frameBuffer = frameBuffer;
        this.renderer = renderer;
        this.textRenderer = textRenderer;
        this.shader = Shaders.getTextureShader();
        this.textShader = Shaders.getTextShader();
        this.blitShader = new BlitShader();
        this.combined = new Matrix4f().identity();
        this.viewportSize = viewportSize;
        this.uiOutlinePipeline = new PostPassPipeline<>(
                new UiOutlineShader(),
                frameBuffer,
                false,
                false
        );
    }

    private static final Vector4f resetColour = new Vector4f(1);

    /**
     * @param position <b>CENTER</b> point of the box
     */
    public void renderCenteredBox(Vector3fc position, Vector3fc size, Vector4fc colour) {
        shader.setUniform("colour", colour, false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderer.renderBox(position, size, shader, combined);
        glBlendFunc(GL_ONE, GL_ZERO);
        shader.setUniform("colour", resetColour, false);
    }

    public void renderBox(Vector3fc position, Vector3fc size, Vector4fc colour) {
        shader.setUniform("colour", colour, false);
        var topLeftPosition = new Vector3f(size).div(2).add(position);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderer.renderBox(topLeftPosition, size, shader, combined);
        glBlendFunc(GL_ONE, GL_ZERO);
        shader.setUniform("colour", resetColour, false);
    }

    public @Nullable Vector2f getAbsoluteCursorPosition(Vector2ic cursorPosition, String text, Font font, Vector2fc size) {
        return textRenderer.getAbsoluteCursorPosition(cursorPosition, text, font, size);
    }

    public void renderBackground(Vector2fc position,
                                 Vector2fc size,
                                 Vector4fc backgroundColour,
                                 float borderTickness,
                                 Vector4fc borderColour) {
        var shader = uiOutlinePipeline.getShader();
        shader.setPosition(position.div(new Vector2f(viewportSize), new Vector2f()).mul(2, -2).sub(1f, -1f));
        shader.setSize(size.div(new Vector2f(viewportSize), new Vector2f()).mul(2, -2));
        shader.setBackgroundColour(backgroundColour);
        shader.setBorderThickness(new Vector2f(borderTickness).div(new Vector2f(viewportSize)));
        shader.setBorderColour(borderColour);

        renderer.render(uiOutlinePipeline);
    }

    public void renderText(String text, Font font, Vector2fc position) {
        renderText(text, font, position, null);
    }

    /**
     * @param cursorPosition will be set to the relative cursor position after rendering the text
     */
    public void renderText(String text, Font font, Vector2fc position, @Nullable Vector2f cursorPosition) {
        textRenderer.render(text, font, position, textShader, combined, cursorPosition);
    }

    /**
     * @param size the borders of the text field, after which the text will be wrapped
     */
    public void renderText(String text, Font font, Vector2fc position, Vector2fc size) {
        renderText(text, font, position, size, null);
    }

    /**
     * @param size           the borders of the text field, after which the text will be wrapped
     * @param cursorPosition returns the relative cursor position after rendering the text
     */
    public void renderText(String text, Font font, Vector2fc position, Vector2fc size, @Nullable Vector2f cursorPosition) {
        textRenderer.render(text, font, position, size, textShader, combined, cursorPosition);
    }

    public void render(Pipeline<?> pipeline) {
        renderer.render(pipeline);
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
        renderer.getContext().checkThread$engine();

        blitShader.setSprite(texture);
        blitShader.setUseSpriteColour(true);
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

    public Vector2f ndcToScreenSpace(Vector2f ndc) {
        return new Vector2f(ndc).div(2).add(0.5f, 0.5f).mul(viewportSize.x(), viewportSize.y());
    }

    public Batch setShader(ShaderProgram shader) {
        this.shader = shader;
        return this;
    }

    public Matrix4fc getCombined() {
        return combined;
    }

    public Batch setCombined(Matrix4fc mat) {
        this.combined.set(mat);
        return this;
    }

    @SuppressWarnings("LombokGetterMayBeUsed") //kotlin
    public Vector2ic getViewportSize() {
        return viewportSize;
    }

    @SuppressWarnings("LombokGetterMayBeUsed") //kotlin
    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

}

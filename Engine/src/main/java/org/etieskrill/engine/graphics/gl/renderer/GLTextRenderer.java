package org.etieskrill.engine.graphics.gl.renderer;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.TextRenderer;
import org.etieskrill.engine.graphics.gl.BufferObject.Frequency;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.VertexArrayObject;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.text.BitmapFont;
import org.etieskrill.engine.graphics.text.Font;
import org.etieskrill.engine.graphics.text.Glyph;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNullElse;
import static org.lwjgl.opengl.GL46C.*;

public class GLTextRenderer extends GLDebuggableRenderer implements TextRenderer, Disposable {

    public static final int MAX_BATCH_LENGTH = 1 << 10; //Max text length is 1024 characters per draw call

    private final List<RenderedGlyph> renderedGlyphs = new ArrayList<>(MAX_BATCH_LENGTH);
    private final VertexArrayObject<RenderedGlyph> glyphVAO = VertexArrayObject
            .builder(RenderedGlyphAccessor.getInstance())
            .numVertexElements((long) MAX_BATCH_LENGTH)
            .frequency(Frequency.STREAM)
            .build();

    public GLTextRenderer() {
        for (int i = 0; i < MAX_BATCH_LENGTH; i++)
            renderedGlyphs.add(new RenderedGlyph());
    }

    @Override
    public void render(String chars, Font font, Vector2fc position, ShaderProgram shader, Matrix4fc combined, @Nullable Vector2f cursorPosition) {
        render(chars, font, position, null, shader, combined, cursorPosition);
    }

    @Override
    public void render(
            String chars,
            Font font,
            Vector2fc position,
            @Nullable Vector2fc size,
            ShaderProgram shader,
            Matrix4fc combined,
            @Nullable Vector2f cursorPosition
    ) {
        GLUtils.clearError();

        //can also be started just before rendering... but without it, a previously used shader is bound sometimes,
        //even though the uniform setters already bind the shader, so this shouldn't be necessary - literal heresy
        shader.start();

        shader.setUniform("combined", combined, false);
        shader.setUniform("glyphTextureSize", new Vector2f(font.getPixelSize()), false);

        if (font instanceof BitmapFont bitmapFont)
            renderBitmap(chars, bitmapFont, position, size, shader, cursorPosition);
        else throw new UnsupportedOperationException( //TODO ttf
                "Rendering font of type " + font.getClass().getSimpleName() + " is currently not supported");

        GLUtils.checkError("Error drawing glyphs");
    }

    private void renderBitmap(
            String chars,
            BitmapFont font,
            Vector2fc position,
            @Nullable Vector2fc size,
            ShaderProgram shader,
            @Nullable Vector2f cursorPosition
    ) {
        int renderedGlyphIndex = 0;
        Vector2f pen = new Vector2f(0);
        Vector2f penPosition = new Vector2f();
        for (Glyph glyph : font.getGlyphs(chars)) {
            switch (requireNonNullElse(glyph.getCharacter(), (char) 0)) {
                case '\n' -> {
                    pen.set(0, pen.y() + font.getLineHeight());
                    continue;
                }
                default -> {
                    //TODO this is only the most primitive of wrapping; maybe add wrap mode enum? callback, even?
                    if (size != null && (pen.x + glyph.getAdvance().x() > size.x())) { //special chars probably shouldn't get wrapped, right?
                        pen.set(0, pen.y + font.getLineHeight());
                    }
                }
            }

            penPosition
                    .set(position).add(pen)
                    .add(glyph.getPosition());

            var renderedGlyph = renderedGlyphs.get(renderedGlyphIndex++);
            renderedGlyph.getSize().set(glyph.getSize());
            renderedGlyph.getPosition().set(penPosition);
            renderedGlyph.setTextureIndex(glyph.getTextureIndex());

            pen.add(glyph.getAdvance());
        }

        if (cursorPosition != null) cursorPosition.set(pen);

        renderBitmapGlyphs(chars.length(), renderedGlyphs, font, shader);
    }

    private void renderBitmapGlyphs(int numChars, List<RenderedGlyph> renderedGlyphs, BitmapFont font, ShaderProgram shader) {
        glyphVAO.bind();
        glyphVAO.setVertices(renderedGlyphs); //TODO have a looksy at how SSBOs work
        shader.setTexture("glyphs", font.getTextures(), false);

        //TODO invert architecture somehow or split general purpose pipeline renderer off and parent all
//        var glyphPipeline = new Pipeline<>(
//                glyphVAO,
//                new PipelineInfo(
//                        BlendFunction.SOURCE_ALPHA,
//                        PrimitiveType.POINTS,
//                        CullingMode.NONE,
//                        null
//                ),
//                shader, null);
//
//        renderer.render(glyphPipeline);

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); //TODO transparency really belongs to a texture, not a model
        glDrawArrays(GL_POINTS, 0, numChars);
        glBlendFunc(GL_ONE, GL_ZERO);
        glEnable(GL_CULL_FACE);

        renderCalls++;
    }

    @Override
    public void dispose() {
        glyphVAO.dispose();
    }

}

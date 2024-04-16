package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.TextRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.texture.font.BitmapFont;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.etieskrill.engine.graphics.texture.font.Glyph;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNullElse;
import static org.lwjgl.opengl.GL46C.*;

public class GLTextRenderer extends DebuggableRenderer implements TextRenderer {

    public static final int MAX_BATCH_LENGTH = 1 << 10; //Max text length is 1024 characters per draw call

    private static final int
            GLYPH_SIZE_BYTES = 2 * Float.BYTES, //TODO pass to buffer object on initialisation
            GLYPH_POSITION_BYTES = 2 * Float.BYTES,
            GLYPH_TEXTURE_INDEX_BYTES = Integer.BYTES,
            GLYPH_TRANSFER_BYTES = GLYPH_SIZE_BYTES + GLYPH_POSITION_BYTES + GLYPH_TEXTURE_INDEX_BYTES;

    private final ByteBuffer glyphBuffer =
            BufferUtils.createByteBuffer(MAX_BATCH_LENGTH * GLYPH_TRANSFER_BYTES);

    @Override
    public void render(String chars, Font font, Vector2fc position, ShaderProgram shader, Matrix4fc combined) {
        GLUtils.clearError();

        shader.setUniform("uCombined", combined, false);
        shader.setUniform("uGlyphTextureSize", new Vector2f(font.getPixelSize()), false);

        if (font instanceof BitmapFont bitmapFont) renderBitmap(chars, bitmapFont, position, shader);
        else throw new UnsupportedOperationException( //TODO ttf
                "Rendering font of type " + font.getClass().getSimpleName() + " is currently not supported");
    }

    private void renderBitmap(String chars, BitmapFont font, Vector2fc position, ShaderProgram shader) {
        glyphBuffer.rewind().limit(chars.length() * GLYPH_TRANSFER_BYTES);

        Vector2f pen = new Vector2f(0);
        Vector2f penPosition = new Vector2f();
        for (Glyph glyph : font.getGlyphs(chars)) {
            switch (requireNonNullElse(glyph.getCharacter(), (char) 0)) {
                case '\n' -> {
                    pen.set(0, pen.y() + font.getLineHeight());
                    continue;
                }
            }

            glyph.getSize()
                    .get(glyphBuffer).position(glyphBuffer.position() + 2 * Float.BYTES);
            penPosition
                    .set(position).add(pen)
                    .add(glyph.getPosition())
                    .get(glyphBuffer).position(glyphBuffer.position() + 2 * Float.BYTES);
            glyphBuffer.putInt(glyph.getTextureIndex());
            pen.add(glyph.getAdvance());
        }

        renderBitmapGlyphs(chars.length(), glyphBuffer.rewind(), font, shader);
        GLUtils.checkError("Error drawing bitmap glyphs");
    }

    private void renderBitmapGlyphs(int numChars, ByteBuffer buffer, BitmapFont font, ShaderProgram shader) {
        bufferBitmapGlyphs(buffer);
        font.getTextures().bind(0);
        shader.setUniform("glyphs", 0, false);

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); //TODO transparency really belongs to a texture, not a model
        glDrawArrays(GL_POINTS, 0, numChars);
        glBlendFunc(GL_ONE, GL_ZERO);
        glEnable(GL_CULL_FACE);

        renderCalls++;
    }

    private void bufferBitmapGlyphs(ByteBuffer buffer) {
        //TODO have a looksy at how SSBOs work
        bindGlyphVAO();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
    }

    private int glyphVAO = -1;

    private void bindGlyphVAO() {
        if (glyphVAO == -1) {
            GLUtils.clearError();

            glyphVAO = glCreateVertexArrays();
            if (glyphVAO == -1)
                throw new IllegalStateException("Could not initialize vertex array");
            glBindVertexArray(glyphVAO);

            int glyphVBO = glCreateBuffers();
            if (glyphVBO == -1)
                throw new IllegalStateException("Could not initialize buffer");

            int position = glyphBuffer.position(); //Reset buffer to original state for gl buffer initialisation
            int limit = glyphBuffer.limit();
            glyphBuffer.rewind();
            glyphBuffer.limit(glyphBuffer.capacity());

            glBindBuffer(GL_ARRAY_BUFFER, glyphVBO);
            glBufferData(GL_ARRAY_BUFFER, glyphBuffer, GL_DYNAMIC_DRAW);
            GLUtils.checkErrorThrowing("Failed to buffer data");

            glyphBuffer.position(position).limit(limit); //Return buffer to state before initialisation

            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, GLYPH_TRANSFER_BYTES, 0L);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, GLYPH_TRANSFER_BYTES, GLYPH_SIZE_BYTES);
            glEnableVertexAttribArray(2);
            glVertexAttribIPointer(2, 1, GL_INT, GLYPH_TRANSFER_BYTES, GLYPH_SIZE_BYTES + GLYPH_POSITION_BYTES);
            GLUtils.checkErrorThrowing("Failed to set vertex array attributes");
        }

        glBindVertexArray(glyphVAO);
    }

}

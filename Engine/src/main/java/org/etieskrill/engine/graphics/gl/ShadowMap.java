package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.gl.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector2ic;
import org.joml.Vector4f;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.DEPTH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.SHADOW;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping.CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL46C.*;

public class ShadowMap extends FrameBuffer {

    private final Texture2D texture;

    protected ShadowMap(Vector2ic size, Texture2D texture) {
        super(size);
        this.texture = texture;
    }

    public static ShadowMap generate(Vector2ic size) {
        return new ShadowMap.Builder(size).build();
    }

    public static class Builder extends FrameBuffer.Builder {
        private final Texture2D texture =
                new Texture2D.BlankBuilder(size)
                        .setFormat(DEPTH)
                        .setType(SHADOW)
                        .setWrapping(CLAMP_TO_BORDER)
                        .setBorderColour(new Vector4f(1.0f))
                        .build();

        public Builder(Vector2ic size) {
            super(size);
            texture.bind(0);
            attach(texture, BufferAttachmentType.DEPTH);
        }

        @Override
        public ShadowMap build() {
            GLUtils.clearError();
            ShadowMap shadowMap = new ShadowMap(size, texture);
            addAttachments(shadowMap);
            return shadowMap;
        }
    }

    @Override
    public void bind(Binding binding) {
        super.bind(binding);
        glReadBuffer(GL_NONE);
        glDrawBuffer(GL_NONE);
        glViewport(0, 0, getSize().x(), getSize().y());
    }

    public Texture2D getTexture() {
        return texture;
    }
}

package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.CubeMapTexture;
import org.joml.Vector2ic;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.DEPTH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.SHADOW;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping.CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;

public class PointShadowMap extends FrameBuffer {

    private final CubeMapTexture texture;

    protected PointShadowMap(Vector2ic size, CubeMapTexture texture) {
        super(size);
        this.texture = texture;
    }

    public static PointShadowMap generate(Vector2ic size) {
        return new PointShadowMap.Builder(size).build();
    }

    public static class Builder extends FrameBuffer.Builder {
        private final CubeMapTexture texture =
                new CubeMapTexture.MemoryBuilder(size)
                        .setFormat(DEPTH)
                        .setType(SHADOW)
                        .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
                        .setWrapping(CLAMP_TO_BORDER)
                        .setBorderColour(new Vector4f(1.0f))
                        .build();

        public Builder(Vector2ic size) {
            super(size);
            texture.bind(0);
            attach(texture, BufferAttachmentType.DEPTH);
        }

        @Override
        public PointShadowMap build() {
            GLUtils.clearError();
            PointShadowMap pointShadowMap = new PointShadowMap(size, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
            addAttachments(pointShadowMap);
            return pointShadowMap;
        }
    }

    @Override
    public void bind(Binding binding) {
        super.bind(binding);
        glReadBuffer(GL_NONE);
        glDrawBuffer(GL_NONE);
        glViewport(0, 0, getSize().x(), getSize().y());
    }

    public CubeMapTexture getTexture() {
        return texture;
    }

}

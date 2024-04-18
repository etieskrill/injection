package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.CubeMapArrayTexture;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;
import org.joml.Vector4f;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.DEPTH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.SHADOW;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping.CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;

public class PointShadowMapArray extends FrameBuffer {

    private final CubeMapArrayTexture texture;

    protected PointShadowMapArray(Vector2ic size, CubeMapArrayTexture texture) {
        super(size);
        this.texture = texture;
    }

    public static PointShadowMapArray generate(@NotNull Vector2ic size, int length) {
        return new PointShadowMapArray.Builder(size, length).build();
    }

    public static class Builder extends FrameBuffer.Builder {
        private final int length;
        private final CubeMapArrayTexture texture;

        public Builder(@NotNull Vector2ic size, int length) {
            super(size);

            this.length = length;

            texture = new CubeMapArrayTexture.BlankBuilder(size, length)
                    .setFormat(DEPTH)
                    .setType(SHADOW)
                    .setMipMapping(MinFilter.LINEAR, MagFilter.LINEAR)
                    .setWrapping(CLAMP_TO_EDGE)
                    .setBorderColour(new Vector4f(1.0f))
                    .build();
            texture.bind(0);
            attach(texture, BufferAttachmentType.DEPTH);
        }

        @Override
        public PointShadowMapArray build() {
            GLUtils.clearError();
            PointShadowMapArray pointShadowMap = new PointShadowMapArray(size, texture);
            pointShadowMap.bind();
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
            glReadBuffer(GL_NONE);
            glDrawBuffer(GL_NONE);
            addAttachments(pointShadowMap);
            return pointShadowMap;
        }
    }

    @Override
    public void bind(Binding binding) {
        super.bind(binding);
        glViewport(0, 0, getSize().x(), getSize().y());
    }

    public CubeMapArrayTexture getTexture() {
        return texture;
    }

}

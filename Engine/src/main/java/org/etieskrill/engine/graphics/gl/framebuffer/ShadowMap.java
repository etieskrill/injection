package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.Vector2ic;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;

/**
 * TODO this is not true, using depth comparison without shadow samplers causes undefined behaviour
 * It is possible to use both a regular sampler (sampler{*D,Cube}) or a shadow sampler (sampler{*D,Cube}Shadow) in glsl
 * code, as the {@link ShadowMap#texture} has the relevant comparator mode set up. The shadow sampler is slightly easier
 * to use, and creates better results out of the box.
 */
public abstract class ShadowMap<T extends AbstractTexture & FrameBufferAttachment> extends FrameBuffer {

    protected final T texture;

    protected ShadowMap(Vector2ic size, T texture) {
        super(size, GL_DEPTH_BUFFER_BIT);
        this.texture = texture;
    }

    public static abstract class Builder<T extends AbstractTexture & FrameBufferAttachment> extends FrameBuffer.Builder {
        protected T texture;

        public Builder(Vector2ic size) {
            super(size);
            texture = generateTexture();
            attach(texture, BufferAttachmentType.DEPTH);
        }

        /**
         * This constructor is for textures (e.g. array), which need additional arguments.
         */
        protected Builder() {
            super(null);
        }

        protected abstract T generateTexture();

        protected abstract ShadowMap<T> createShadowMap();

        @Override
        public ShadowMap<T> build() {
            GLUtils.clearError();
            ShadowMap<T> shadowMap = createShadowMap();
            shadowMap.bind();
            texture.bind(0);
            glTexParameteri(texture.getTarget().gl(), GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glTexParameteri(texture.getTarget().gl(), GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
            glReadBuffer(GL_NONE);
            glDrawBuffer(GL_NONE);
            addAttachments(shadowMap);
            return shadowMap;
        }
    }

    @Override
    public void bind(Binding binding) {
        super.bind(binding);
        glViewport(0, 0, getSize().x(), getSize().y());
    }

    public T getTexture() {
        return texture;
    }

}

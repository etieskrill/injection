package org.etieskrill.engine.graphics.gl.framebuffer;

import io.github.etieskrill.injection.extension.shader.TextureShadow;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.Vector2ic;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;

/**
 * It is possible to use a regular {@link AbstractTexture texture} (with a {@code sampler{*D,Cube}}) as a shadow map,
 * but this introduces unnecessary wrangling with colour vectors, among other inconveniences. Instead, leverage a
 * shadow sampler ({@code sampler{*D,Cube}Shadow}) by using this class.
 * <p>
 * Note that; while it is possible on various hardware to read from a shadow/depth texture using a non-shadow sampler
 * and vice versa, and this class effectively only acts as a proxy for the {@link ShadowMap#texture}, this action causes
 * undefined behaviour according to the specification.
 */
public abstract class ShadowMap<T extends AbstractTexture & FrameBufferAttachment> extends FrameBuffer implements TextureShadow {

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
        glViewport(0, 0, getSize().x(), getSize().y()); //TODO do in framebuffer?
    }

    @Override
    public void bind(int unit) {
        texture.bind(unit);
    }

    @Override
    public void unbind(int unit) {
        texture.unbind(unit);
    }

    public T getTexture() {
        return texture;
    }

}

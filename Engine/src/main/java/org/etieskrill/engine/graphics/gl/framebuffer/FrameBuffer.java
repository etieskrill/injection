package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Vector2ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.etieskrill.engine.util.ClassUtils.getSimpleName;
import static org.lwjgl.opengl.GL33C.*;

public class FrameBuffer implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(FrameBuffer.class);

    private final int fbo;
    private final Vector2ic size;
    private final Map<BufferAttachmentType, FrameBufferAttachment> attachments;

    public static FrameBuffer getStandard(Vector2ic size) {
        // Colour buffer as a texture attachment
        Texture2D colourBufferTexture = Textures.genBlank(size, Texture2D.Format.RGB);

        // Depth and stencil buffer as a renderbuffer attachment
        RenderBuffer depthStencilBuffer = new RenderBuffer(size, RenderBuffer.Type.DEPTH_STENCIL);

        return new Builder(size)
                .attach(colourBufferTexture, BufferAttachmentType.COLOUR0)
                .attach(depthStencilBuffer, BufferAttachmentType.DEPTH_STENCIL)
                .build();
    }

    public static class Builder {
        protected final Vector2ic size;

        private final Map<BufferAttachmentType, FrameBufferAttachment> attachments = new HashMap<>();

        public Builder(Vector2ic size) {
            this.size = size;
        }

        public Builder attach(FrameBufferAttachment attachment, BufferAttachmentType type) {
            attachments.put(type, attachment);
            return this;
        }

        public FrameBuffer build() {
            GLUtils.clearError();
            FrameBuffer frameBuffer = new FrameBuffer(size);
            addAttachments(frameBuffer);
            return frameBuffer;
        }

        protected void addAttachments(FrameBuffer frameBuffer) {
            frameBuffer.bind();
            for (BufferAttachmentType type : attachments.keySet()) {
                FrameBufferAttachment attachment = attachments.get(type);
                if (attachment == null) continue;
                if (!attachment.getSize().equals(this.size)) {
                    logger.warn("Skipping attachment because sizes do not match; buffer size: {}, attachment size: {}",
                            this.size, attachment.getSize());
                    continue;
                }

                switch (attachment) {
                    case Texture2D texture2D -> glFramebufferTexture2D(
                            GL_FRAMEBUFFER,
                            type.toGLAttachment(),
                            GL_TEXTURE_2D,
                            texture2D.getID(),
                            0);
                    case RenderBuffer renderBuffer -> glFramebufferRenderbuffer(
                            GL_FRAMEBUFFER,
                            type.toGLAttachment(),
                            GL_RENDERBUFFER,
                            renderBuffer.getID());
                    default -> throw new FrameBufferCreationException(
                            "Unknown framebuffer attachment type: " + getSimpleName(attachment)
                    );
                }

                if (glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                    throw new FrameBufferCreationException("Incomplete framebuffer attachment for " +
                            type.name() + ": " + attachment);
                }
            }

            int ret;
            if ((ret = glCheckFramebufferStatus(GL_FRAMEBUFFER)) != GL_FRAMEBUFFER_COMPLETE) {
                throw new FrameBufferCreationException("Framebuffer was not successfully completed", ret);
            }

            GLUtils.checkErrorThrowing("Error during framebuffer creation");

            frameBuffer.getAttachments().putAll(attachments);
        }
    }

    protected FrameBuffer(Vector2ic size) {
        this.fbo = glGenFramebuffers();
        this.size = size;
        this.attachments = new HashMap<>(3);
    }

    public enum Binding {
        READ,
        WRITE,
        BOTH
    }

    public void bind() {
        bind(Binding.BOTH);
    }

    public void bind(Binding binding) {
        glBindFramebuffer(switch (binding) {
            case READ -> GL_READ_FRAMEBUFFER;
            case WRITE -> GL_DRAW_FRAMEBUFFER;
            case BOTH -> GL_FRAMEBUFFER;
        }, fbo);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public Vector2ic getSize() {
        return size;
    }

    public Map<BufferAttachmentType, FrameBufferAttachment> getAttachments() {
        return attachments;
    }

    public FrameBufferAttachment getAttachment(BufferAttachmentType type) {
        return attachments.get(type);
    }

    @Override
    public void dispose() {
        glDeleteFramebuffers(fbo);
    }

}

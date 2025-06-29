package org.etieskrill.engine.graphics.gl.framebuffer;

import lombok.Getter;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.GLUtils;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType.*;
import static org.lwjgl.opengl.GL33C.*;

public class FrameBuffer implements io.github.etieskrill.injection.extension.shader.dsl.FrameBuffer, Disposable {

    private final int fbo;
    private final @Getter Vector2ic size;
    private final @Getter Map<BufferAttachmentType, FrameBufferAttachment> attachments;
    private final int glBufferClearMask;
    private final int @Nullable [] glColourDrawBuffers;
    private final @Getter Vector4f clearColour;

    private static final Logger logger = LoggerFactory.getLogger(FrameBuffer.class);

    public static FrameBuffer getStandard(Vector2ic size) {
        // Colour buffer as a texture attachment
        Texture2D colourBufferTexture = Textures.genBlank(size, Texture2D.Format.RGB);

        // Depth and stencil buffer as a renderbuffer attachment
        RenderBuffer depthStencilBuffer = new RenderBuffer(size, RenderBuffer.Type.DEPTH_STENCIL);

        return new Builder(size)
                .attach(colourBufferTexture, COLOUR0)
                .attach(depthStencilBuffer, DEPTH_STENCIL)
                .build();
    }

    public static class Builder {
        protected Vector2ic size;
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
            int glBufferClearMask =
                    getBufferBit(GL_COLOR_BUFFER_BIT, COLOUR0, COLOUR1, COLOUR2, COLOUR3, COLOUR31)
                    | getBufferBit(GL_DEPTH_BUFFER_BIT, DEPTH, DEPTH_STENCIL)
                    | getBufferBit(GL_STENCIL_BUFFER_BIT, STENCIL, DEPTH_STENCIL);
            int[] glColourDrawBuffers = attachments.keySet().stream()
                    .filter(attachment -> attachment.toGLAttachment() >= COLOUR0.toGLAttachment()
                                          && attachment.toGLAttachment() <= COLOUR31.toGLAttachment())
                    .mapToInt(BufferAttachmentType::toGLAttachment)
                    .sorted()
                    .toArray();
            FrameBuffer frameBuffer = new FrameBuffer(requireNonNull(size), glBufferClearMask, glColourDrawBuffers);
            addAttachments(frameBuffer);
            return frameBuffer;
        }

        private int getBufferBit(int bitFlag, BufferAttachmentType... attachmentTypes) {
            for (BufferAttachmentType attachmentType : attachmentTypes) {
                if (attachments.containsKey(attachmentType)) {
                    return bitFlag;
                }
            }
            return 0;
        }

        /**
         * This method may only be called after a framebuffer object has been generated.
         */
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

                attachment.attach(type);

                if (glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                    throw new FrameBufferCreationException("Incomplete framebuffer attachment for " +
                                                           type.name() + ": " + attachment);
                }
            }

            int ret;
            if ((ret = glCheckFramebufferStatus(GL_FRAMEBUFFER)) != GL_FRAMEBUFFER_COMPLETE) {
                var message = switch (ret) {
                    case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "Incomplete attachment";
                    case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "Missing attachment";
                    case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "Incomplete draw buffer";
                    case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "Incomplete read buffer";
                    case GL_FRAMEBUFFER_UNSUPPORTED -> "Unsupported framebuffer";
                    case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "Incomplete multisample buffer";
                    case GL_FRAMEBUFFER_UNDEFINED -> "Undefined framebuffer";
                    default -> "Unknown framebuffer error: " + ret;
                };
                throw new FrameBufferCreationException("Framebuffer was not successfully completed: " + message);
            }

            GLUtils.checkErrorThrowing("Error during framebuffer creation");

            frameBuffer.getAttachments().putAll(attachments);
        }
    }

    protected FrameBuffer(Vector2ic size, int glBufferClearMask) {
        this(size, glBufferClearMask, null);
    }

    protected FrameBuffer(Vector2ic size, int glBufferClearMask, int @Nullable [] glColourDrawBuffers) {
        this.fbo = glGenFramebuffers();
        this.size = size;
        this.glBufferClearMask = glBufferClearMask;
        this.glColourDrawBuffers = glColourDrawBuffers;
        this.clearColour = new Vector4f();
        this.attachments = new HashMap<>(3);
    }

    public enum Binding {
        READ,
        WRITE,
        BOTH
    }

    @Override
    public void bind() {
        bind(Binding.BOTH);
    }

    public void bind(Binding binding) {
        glBindFramebuffer(switch (binding) {
            case READ -> GL_READ_FRAMEBUFFER;
            case WRITE -> GL_DRAW_FRAMEBUFFER;
            case BOTH -> GL_FRAMEBUFFER;
        }, fbo);
        if (glColourDrawBuffers != null) {
            glDrawBuffers(glColourDrawBuffers);
        }
    }

    public void unbind() {
        if (glColourDrawBuffers != null) {
            glDrawBuffers(COLOUR0.toGLAttachment());
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public static void bindScreenBuffer() {
        glDrawBuffers(COLOUR0.toGLAttachment());
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void clear() {
        bind();
        glClearColor(clearColour.x, clearColour.y, clearColour.z, clearColour.w);
        glClear(glBufferClearMask);
        unbind();
    }

    public static void clearScreenBuffer() {
        clearScreenBuffer(null);
    }

    public static void clearScreenBuffer(@Nullable Vector4fc clearColour) {
        bindScreenBuffer();
        if (clearColour != null) glClearColor(clearColour.x(), clearColour.y(), clearColour.z(), clearColour.w());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public void setClearColour(Vector4fc clearColour) {
        this.clearColour.set(clearColour);
    }

    public FrameBufferAttachment getAttachment(BufferAttachmentType type) {
        return attachments.get(type);
    }

    @Override
    public void dispose() {
        glDeleteFramebuffers(fbo);
        attachments.values().forEach(Disposable::dispose);
    }

}

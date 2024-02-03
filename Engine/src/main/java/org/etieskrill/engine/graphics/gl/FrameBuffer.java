package org.etieskrill.engine.graphics.gl;

import org.joml.Vector2i;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Vector2ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

public class FrameBuffer implements Disposable {
    
    private static final Logger logger = LoggerFactory.getLogger(FrameBuffer.class);
    
    private final int fbo;
    private final Vector2ic size;
    private final Map<AttachmentType, FrameBufferAttachment> attachments;
    
    public static FrameBuffer getStandard(Vector2ic size) {
        // Colour buffer as a texture attachment
        Texture2D colourBufferTexture = Textures.genBlank(size, Texture2D.Format.RGB);
    
        // Depth and stencil buffer as a renderbuffer attachment
        RenderBuffer depthStencilBuffer = new RenderBuffer(size, RenderBuffer.Type.DEPTH_STENCIL);
        
        return new Builder(size)
                .attach(colourBufferTexture, AttachmentType.COLOUR0)
                .attach(depthStencilBuffer, AttachmentType.DEPTH_STENCIL)
                .build();
    }
    
    public static final class Builder {
        private Vector2ic size;
        
        private final Map<AttachmentType, FrameBufferAttachment> attachments = new HashMap<>();
        
        public Builder(Vector2ic size) {
            this.size = size;
        }
    
        public Builder attach(FrameBufferAttachment attachment, AttachmentType type) {
            attachments.put(type, attachment);
            return this;
        }
        
        public FrameBuffer build() {
            glGetError();
            int ret;
            
            FrameBuffer frameBuffer = new FrameBuffer(size);
            frameBuffer.bind();
            for (AttachmentType type : attachments.keySet()) {
                FrameBufferAttachment attachment = attachments.get(type);
                if (attachment == null) continue;
                if (!attachment.getSize().equals(this.size)) {
                    logger.warn("Skipping attachment because sizes do not match; buffer size: {}, attachment size: {}",
                            this.size, attachment.getSize());
                    continue;
                }
                
                if (attachment instanceof Texture2D) {
                    glFramebufferTexture2D(GL_FRAMEBUFFER, type.toGLAttachment(), GL_TEXTURE_2D, attachment.getID(), 0);
                } else if (attachment instanceof RenderBuffer) {
                    glFramebufferRenderbuffer(GL_FRAMEBUFFER, type.toGLAttachment(), GL_RENDERBUFFER, attachment.getID());
                }
                
                if (glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
                    throw new IllegalStateException("Incomplete framebuffer attachment for " +
                            type.name() + ": " + attachment);
            }
            
            if ((ret = glCheckFramebufferStatus(GL_FRAMEBUFFER)) != GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Framebuffer was not successfully completed: 0x" +
                        Integer.toHexString(ret).toUpperCase());
    
            if ((ret = glGetError()) != GL_NO_ERROR)
                throw new IllegalStateException("Error during framebuffer creation: 0x" +
                        Integer.toHexString(ret).toUpperCase());
            
            frameBuffer.getAttachments().putAll(attachments);
            
            return frameBuffer;
        }
    }
    
    private FrameBuffer(Vector2ic size) {
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
    
    public enum AttachmentType {
        COLOUR0,
        COLOUR1,
        COLOUR2,
        COLOUR3,
        DEPTH,
        STENCIL,
        DEPTH_STENCIL;
        
        public int toGLAttachment() {
            return switch (this) {
                case COLOUR0 -> GL_COLOR_ATTACHMENT0;
                case COLOUR1 -> GL_COLOR_ATTACHMENT1;
                case COLOUR2 -> GL_COLOR_ATTACHMENT2;
                case COLOUR3 -> GL_COLOR_ATTACHMENT3;
                case DEPTH -> GL_DEPTH_ATTACHMENT;
                case STENCIL -> GL_STENCIL_ATTACHMENT;
                case DEPTH_STENCIL -> GL_DEPTH_STENCIL_ATTACHMENT;
            };
        }
    }
    
    public Vector2ic getSize() {
        return size;
    }
    
    public Map<AttachmentType, FrameBufferAttachment> getAttachments() {
        return attachments;
    }
    
    public FrameBufferAttachment getAttachment(AttachmentType type) {
        return attachments.get(type);
    }
    
    @Override
    public void dispose() {
        glDeleteFramebuffers(fbo);
    }
    
}

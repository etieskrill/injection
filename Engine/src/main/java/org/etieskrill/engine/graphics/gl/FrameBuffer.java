package org.etieskrill.engine.graphics.gl;

import glm_.vec2.Vec2i;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.texture.Texture;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

public class FrameBuffer implements Disposable {
    
    private final int fbo;
    private final Vec2i size;
    private final Map<AttachmentType, FrameBufferAttachment> attachments;
    
    public static FrameBuffer getStandard(Vec2i size) {
        // Colour buffer as a texture attachment
        Texture colourBufferTexture = Texture.genBlank(size, Texture.Format.RGB);
    
        // Depth and stencil buffer as a renderbuffer attachment
        RenderBuffer depthStencilBuffer = new RenderBuffer(size, RenderBuffer.Type.DEPTH_STENCIL);
        
        return new Builder(size)
                .attach(colourBufferTexture, AttachmentType.COLOUR0)
                .attach(depthStencilBuffer, AttachmentType.DEPTH_STENCIL)
                .build();
    }
    
    public static final class Builder {
        private final Vec2i size;
        
        private final Map<AttachmentType, FrameBufferAttachment> attachments = new HashMap<>();
        
        public Builder(Vec2i size) {
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
                if (attachment instanceof Texture texture) {
                    glFramebufferTexture2D(GL_FRAMEBUFFER, type.toGLAttachment(), GL_TEXTURE_2D, texture.getID(), 0);
                } else if (attachment instanceof RenderBuffer buffer) {
                    buffer.bind();
                    glFramebufferRenderbuffer(GL_FRAMEBUFFER, type.toGLAttachment(), GL_RENDERBUFFER, buffer.getID());
                    buffer.unbind();
                }
                if ((ret = glCheckFramebufferStatus(GL_FRAMEBUFFER)) == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
                    throw new IllegalStateException("Incomplete framebuffer attachment: " + attachment.toString());
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
    
    private FrameBuffer(Vec2i size) {
        this.fbo = glGenFramebuffers();
        this.size = size;
        this.attachments = new HashMap<>(2);
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
    
    public Vec2i getSize() {
        return size;
    }
    
    public Map<AttachmentType, FrameBufferAttachment> getAttachments() {
        return attachments;
    }
    
    @Override
    public void dispose() {
        glDeleteFramebuffers(fbo);
    }
    
}

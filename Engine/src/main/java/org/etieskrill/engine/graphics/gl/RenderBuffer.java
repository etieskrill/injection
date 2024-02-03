package org.etieskrill.engine.graphics.gl;

import org.joml.Vector2i;
import org.etieskrill.engine.Disposable;
import org.joml.Vector2ic;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_RGBA8;
import static org.lwjgl.opengl.GL30C.*;

public class RenderBuffer implements Disposable, FrameBufferAttachment {
    
    private final Vector2ic size;
    private final int rbo;
    private final Type type;
    
    public enum Type {
        COLOUR,
        DEPTH,
        DEPTH_HIGHP,
        STENCIL,
        DEPTH_STENCIL,
        DEPTH_HIGHP_STENCIL;
        
        public int toGLType() {
            return switch (this) {
                case COLOUR -> GL_RGBA8;
                case DEPTH -> GL_DEPTH_COMPONENT;
                case DEPTH_HIGHP -> GL_DEPTH_COMPONENT32F;
                case STENCIL -> GL_STENCIL_ATTACHMENT;
                case DEPTH_STENCIL -> GL_DEPTH24_STENCIL8;
                case DEPTH_HIGHP_STENCIL -> GL_DEPTH32F_STENCIL8;
            };
        }
    }
    
    public RenderBuffer(Vector2ic size, Type type) {
        this.size = size;
        
        int ret;
        glGetError();
        
        this.rbo = glGenRenderbuffers();
        bind();
        
        this.type = type;
        glRenderbufferStorage(GL_RENDERBUFFER, type.toGLType(), size.x(), size.y());
        
        if ((ret = glGetError()) != GL_NO_ERROR)
            throw new IllegalStateException("Error during renderbuffer creation: 0x" + Integer.toHexString(ret));
    }
    
    public void bind() {
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
    }
    
    public void unbind() {
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }
    
    @Override
    public Vector2ic getSize() {
        return size;
    }
    
    @Override
    public int getID() {
        return rbo;
    }
    
    public Type getType() {
        return type;
    }
    
    @Override
    public void dispose() {
        glDeleteRenderbuffers(rbo);
    }
    
}
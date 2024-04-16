package org.etieskrill.engine.graphics.gl.framebuffer;

import org.joml.Vector2ic;

import static org.lwjgl.opengl.GL30C.*;

public interface FrameBufferAttachment {

    Vector2ic getSize();

    int getID();

    //TODO add bind() and unbind() methods with default impls

    enum BufferAttachmentType {
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

}

package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.Disposable;
import org.joml.Vector2ic;

import static org.lwjgl.opengl.GL30C.*;

public interface FrameBufferAttachment extends Disposable {

    Vector2ic getSize();

    //TODO add bind() and unbind() methods with default impls

    /**
     * Calls for the {@code attachment} to attach itself to the currently bound framebuffer, which is guaranteed to be
     * valid.
     *
     * @param type the binding point in the framebuffer to which the attachment must bind itself
     */
    void attach(BufferAttachmentType type);

    enum BufferAttachmentType {
        COLOUR0(GL_COLOR_ATTACHMENT0),
        COLOUR1(GL_COLOR_ATTACHMENT1),
        COLOUR2(GL_COLOR_ATTACHMENT2),
        COLOUR3(GL_COLOR_ATTACHMENT3),
        COLOUR31(GL_COLOR_ATTACHMENT31),
        DEPTH(GL_DEPTH_ATTACHMENT),
        STENCIL(GL_STENCIL_ATTACHMENT),
        DEPTH_STENCIL(GL_DEPTH_STENCIL_ATTACHMENT);

        private final int glAttachmentType;

        BufferAttachmentType(int glAttachmentType) {
            this.glAttachmentType = glAttachmentType;
        }

        public int toGLAttachment() {
            return glAttachmentType;
        }
    }

}

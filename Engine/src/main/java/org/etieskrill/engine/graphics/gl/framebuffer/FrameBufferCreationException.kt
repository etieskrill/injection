package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.graphics.gl.exception.GLException;

public class FrameBufferCreationException extends GLException {

    public FrameBufferCreationException(String message) {
        super(message);
    }

    public FrameBufferCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FrameBufferCreationException(String message, String infoLog) {
        super(message, infoLog);
    }

    public FrameBufferCreationException(String message, Integer errorCode) {
        super(message, errorCode);
    }

}

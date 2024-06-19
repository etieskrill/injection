package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.gl.exception.GLException;

public class BufferCreationException extends GLException {
    public BufferCreationException(String message) {
        super(message);
    }

    public BufferCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferCreationException(String message, String infoLog) {
        super(message, infoLog);
    }

    public BufferCreationException(String message, Integer errorCode) {
        super(message, errorCode);
    }
}

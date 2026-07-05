package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.gl.exception.GLError;
import org.etieskrill.engine.graphics.gl.exception.GLException;

public class BufferCreationException extends GLException {
    public BufferCreationException(String message) {
        super(message, null, null, null);
    }

    public BufferCreationException(String message, Throwable cause) {
        super(message, cause, null, null);
    }

    public BufferCreationException(String message, String infoLog) {
        super(message, null, infoLog, null);
    }

    public BufferCreationException(String message, Integer errorCode) {
        super(message, null, null, GLError.Companion.fromGL(errorCode));
    }
}

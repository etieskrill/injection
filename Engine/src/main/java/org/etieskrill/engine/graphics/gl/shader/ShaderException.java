package org.etieskrill.engine.graphics.gl.shader;

import org.etieskrill.engine.graphics.gl.exception.GLError;
import org.etieskrill.engine.graphics.gl.exception.GLException;

public class ShaderException extends GLException {

    public ShaderException(String message) {
        super(message, null, null, null);
    }

    public ShaderException(String message, Throwable cause) {
        super(message, cause, null, null);
    }

    public ShaderException(String message, String infoLog) {
        super(message, null, infoLog, null);
    }

    public ShaderException(String message, Integer errorCode) {
        super(message, null, null, GLError.Companion.fromGL(errorCode));
    }

}

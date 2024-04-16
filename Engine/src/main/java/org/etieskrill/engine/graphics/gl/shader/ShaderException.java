package org.etieskrill.engine.graphics.gl.shader;

import org.etieskrill.engine.graphics.gl.exception.GLException;

public class ShaderException extends GLException {

    public ShaderException(String message) {
        super(message);
    }

    public ShaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShaderException(String message, String infoLog) {
        super(message, infoLog);
    }

    public ShaderException(String message, Integer errorCode) {
        super(message, errorCode);
    }

}

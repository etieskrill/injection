package org.etieskrill.engine.graphics.gl.shader;

public class ShaderCreationException extends ShaderException {

    public ShaderCreationException(String message) {
        super(message);
    }

    public ShaderCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShaderCreationException(String message, String infoLog) {
        super(message, infoLog);
    }

    public ShaderCreationException(String message, Integer errorCode) {
        super(message, errorCode);
    }

}

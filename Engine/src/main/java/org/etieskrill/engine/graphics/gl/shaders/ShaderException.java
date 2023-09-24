package org.etieskrill.engine.graphics.gl.shaders;

public class ShaderException extends RuntimeException {

    public ShaderException(String message) {
        super(message);
    }

    public ShaderException(String message, Throwable cause) {
        super(message, cause);
    }

}

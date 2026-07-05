package org.etieskrill.engine.graphics.gl.shader;

public class ShaderUniformException extends ShaderException {

    public ShaderUniformException(String message) {
        super(message);
    }

    public ShaderUniformException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShaderUniformException(String message, String uniformName) {
        super(message, uniformName);
    }

}

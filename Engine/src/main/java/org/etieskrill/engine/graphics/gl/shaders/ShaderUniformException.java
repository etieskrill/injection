package org.etieskrill.engine.graphics.gl.shaders;

public class ShaderUniformException extends ShaderException {

    private String uniformName;

    public ShaderUniformException(String message) {
        super(message);
    }

    public ShaderUniformException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShaderUniformException(String message, String uniformName) {
        super(message);
        this.uniformName = uniformName;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (uniformName != null) message += ": " + uniformName;
        return message;
    }

}

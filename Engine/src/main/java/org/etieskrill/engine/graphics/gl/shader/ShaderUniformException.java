package org.etieskrill.engine.graphics.gl.shader;

import org.jetbrains.annotations.Nullable;

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
    protected @Nullable String getErrorMessage(Integer errorCode) {
        return uniformName;
    }

}

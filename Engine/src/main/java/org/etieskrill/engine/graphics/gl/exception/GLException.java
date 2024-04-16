package org.etieskrill.engine.graphics.gl.exception;

import org.jetbrains.annotations.Nullable;

public class GLException extends RuntimeException {

    protected String infoLog;
    protected Integer errorCode;

    public GLException(String message) {
        super(message);
    }

    public GLException(String message, Throwable cause) {
        super(message, cause);
    }

    public GLException(String message, String infoLog) {
        super(message);
        this.infoLog = infoLog;
    }

    public GLException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (errorCode != null) message += ": 0x%s".formatted(Integer.toHexString(errorCode).toUpperCase());
        String errorMessage = getErrorMessage(errorCode);
        if (errorMessage != null) message += ": " + errorMessage;
        if (infoLog != null) message += "\n" + infoLog;
        return message;
    }

    protected @Nullable String getErrorMessage(Integer errorCode) {
        GLError error = GLError.toError(errorCode);
        return error != null ? error.getMessage() : null;
    }

}


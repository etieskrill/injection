package org.etieskrill.engine.graphics.gl.shaders;

public class ShaderCreationException extends ShaderException {

    private String infoLog;
    private Integer errorCode;

    public ShaderCreationException(String message) {
        super(message);
    }

    public ShaderCreationException(String message, String infoLog) {
        super(message);
        this.infoLog = infoLog;
    }

    public ShaderCreationException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShaderCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (errorCode != null) message += ": 0x%s".formatted(Integer.toHexString(errorCode));
        if (infoLog != null) message += "\n" + infoLog;
        return message;
    }

}

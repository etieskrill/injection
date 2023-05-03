package org.etieskrill.engine.graphics.gl.shaders;

import org.etieskrill.engine.util.ResourceReader;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;

public abstract class ShaderProgram {
    
    private final int programID, vertID, fragID;
    
    public ShaderProgram(String vertexFile, String fragmentFile) {
        programID = GL20C.glCreateProgram();
        
        vertID = loadShader(vertexFile, GL20C.GL_VERTEX_SHADER);
        GL20C.glAttachShader(programID, vertID);
        
        fragID = loadShader(fragmentFile, GL20C.GL_FRAGMENT_SHADER);
        GL20C.glAttachShader(programID, fragID);
        
        GL20C.glLinkProgram(programID);
        GL20C.glValidateProgram(programID);
        
        bindAttributes();
    }
    
    public void start() {
        GL20C.glUseProgram(programID);
    }
    
    public void stop() {
        GL20C.glUseProgram(0);
    }
    
    protected abstract void bindAttributes();
    
    protected void bindAttribute(int attribute, String variableName) {
        GL20C.glBindAttribLocation(programID, attribute, variableName);
    }
    
    private static int loadShader(String file, int shaderType) {
        int shaderID = GL20C.glCreateShader(shaderType);
        GL20C.glShaderSource(shaderID, ResourceReader.getRaw(file));
        GL20C.glCompileShader(shaderID);
        
        if (GL20C.glGetShaderi(shaderID, GL20C.GL_COMPILE_STATUS) != GL11C.GL_TRUE) {
            System.out.println(GL20C.glGetShaderInfoLog(shaderID));
            System.err.println("Failed to compile shader.");
            System.exit(-1);
        }
        
        return shaderID;
    }
    
    public void dispose() {
        stop();
        GL20C.glDetachShader(programID, vertID);
        GL20C.glDeleteShader(vertID);
        GL20C.glDetachShader(programID, fragID);
        GL20C.glDeleteShader(fragID);
        GL20C.glDeleteProgram(programID);
    }
    
}

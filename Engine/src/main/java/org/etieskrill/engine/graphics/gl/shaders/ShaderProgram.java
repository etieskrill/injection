package org.etieskrill.engine.graphics.gl.shaders;

import glm.mat._4.Mat4;
import org.etieskrill.engine.util.ResourceReader;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL33C;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ShaderProgram {
    
    private final int programID, vertID, fragID;
    private final Map<CharSequence, Integer> uniforms;
    
    public ShaderProgram(String vertexFile, String fragmentFile) {
        programID = GL20C.glCreateProgram();
        
        vertID = loadShader(vertexFile, GL20C.GL_VERTEX_SHADER);
        GL20C.glAttachShader(programID, vertID);
        
        fragID = loadShader(fragmentFile, GL20C.GL_FRAGMENT_SHADER);
        GL20C.glAttachShader(programID, fragID);
        
        GL20C.glLinkProgram(programID);
        if (GL20C.glGetProgrami(programID, GL20C.GL_LINK_STATUS) != GL11C.GL_TRUE) {
            System.out.println(GL20C.glGetProgramInfoLog(programID));
            System.err.println("Shader program could not be linked");
        }
        
        disposeShaders();
        
        GL20C.glValidateProgram(programID);
        if (GL20C.glGetProgrami(programID, GL20C.GL_VALIDATE_STATUS) != GL11C.GL_TRUE) {
            System.out.println(GL20C.glGetProgramInfoLog(programID));
            System.err.println("Shader program was not successfully validated");
        }

        this.uniforms = new HashMap<>();
        getUniformLocations();
    }

    protected abstract void getUniformLocations();
    
    public void start() {
        GL20C.glUseProgram(programID);
    }
    
    public void stop() {
        GL20C.glUseProgram(0);
    }

    protected void addUniform(CharSequence name) {
        int uniformLocation = GL33C.glGetUniformLocation(programID, name);
        if (uniformLocation == -1) {
            System.err.printf("Could not find location of uniform with name \"%s\"\n", name);
            return;
        }

        uniforms.put(name, uniformLocation);
    }

    public void setUniformMat4(CharSequence name, boolean transpose, Mat4 mat) {
        GL33C.glUniformMatrix4fv(getUniformLocation(name), transpose, mat.toFa_());
    }
    
    private static int loadShader(String file, int shaderType) {
        int shaderID = GL20C.glCreateShader(shaderType);
        GL20C.glShaderSource(shaderID, ResourceReader.getRaw(file));
        GL20C.glCompileShader(shaderID);
    
        if (GL20C.glGetShaderi(shaderID, GL20C.GL_COMPILE_STATUS) != GL11C.GL_TRUE) {
            System.out.println(GL20C.glGetShaderInfoLog(shaderID));
            String shaderTypeName = "unknown";
            switch (shaderType) {
                case GL33C.GL_VERTEX_SHADER -> shaderTypeName = "vertex";
                case GL33C.GL_FRAGMENT_SHADER -> shaderTypeName = "fragment";
            }
            System.err.println("Failed to compile " + shaderTypeName + " shader");
            System.exit(-1);
        }
        
        return shaderID;
    }
    
    public void disposeShaders() {
        GL20C.glDeleteShader(vertID);
        GL20C.glDeleteShader(fragID);
    }
    
    public void dispose() {
        stop();
        GL20C.glDetachShader(programID, vertID);
        GL20C.glDetachShader(programID, fragID);
        GL20C.glDeleteProgram(programID);
    }

    public int getProgramID() {
        return programID;
    }

    public int getUniformLocation(CharSequence name) {
        Integer location = uniforms.get(name);
        if (location == null) {
            System.err.printf("Uniform of name %s was not registered in the shader\n", name);
            return -1;
        }

        return location;
    }

}

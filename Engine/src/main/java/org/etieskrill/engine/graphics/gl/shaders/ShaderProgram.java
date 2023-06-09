package org.etieskrill.engine.graphics.gl.shaders;

import glm.mat._3.Mat3;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import org.etieskrill.engine.util.ResourceReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

public abstract class ShaderProgram {
    
    private final int programID, vertID, fragID;
    private final Map<CharSequence, Integer> uniforms;
    private final List<CharSequence> unfoundUniforms;
    
    public ShaderProgram(String vertexFile, String fragmentFile) {
        programID = glCreateProgram();
        
        vertID = loadShader(vertexFile, GL_VERTEX_SHADER);
        glAttachShader(programID, vertID);
        
        fragID = loadShader(fragmentFile, GL_FRAGMENT_SHADER);
        glAttachShader(programID, fragID);
    
        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) != GL_TRUE) {
            System.out.println(glGetProgramInfoLog(programID));
            System.err.println("Shader program could not be linked");
        }
        
        disposeShaders();
    
        glValidateProgram(programID);
        if (glGetProgrami(programID, GL_VALIDATE_STATUS) != GL_TRUE) {
            System.out.println(glGetProgramInfoLog(programID));
            System.err.println("Shader program was not successfully validated");
        }

        this.uniforms = new HashMap<>();
        this.unfoundUniforms = new ArrayList<>();
        getUniformLocations();
    }

    protected abstract void getUniformLocations();
    
    public void start() {
        glUseProgram(programID);
    }
    
    public void stop() {
        glUseProgram(0);
    }

    protected void addUniform(CharSequence name) {
        int uniformLocation = glGetUniformLocation(programID, name);
        if (uniformLocation == -1) {
            unfoundUniforms.add(name);
            System.err.printf("[%s] Could not find location of uniform with name \"%s\"\n", this.getClass().getSimpleName(), name);
            return;
        }

        uniforms.put(name, uniformLocation);
    }
    
    public void setUniformInt(CharSequence name, int val) {
        glUniform1i(getUniformLocation(name), val);
    }

    public void setUniformFloat(CharSequence name, float val) {
        glUniform1f(getUniformLocation(name), val);
    }

    public void setUniformVec3(CharSequence name, Vec3 vec) {
        glUniform3fv(getUniformLocation(name), vec.toDfb_());
    }

    public void setUniformVec4(CharSequence name, Vec4 vec) {
        glUniform4fv(getUniformLocation(name), vec.toDfb_());
    }

    public void setUniformMat3(CharSequence name, boolean transpose, Mat3 mat) {
        glUniformMatrix3fv(getUniformLocation(name), transpose, mat.toFa_());
    }

    public void setUniformMat4(CharSequence name, boolean transpose, Mat4 mat) {
        glUniformMatrix4fv(getUniformLocation(name), transpose, mat.toFa_());
    }
    
    private static int loadShader(String file, int shaderType) {
        int shaderID = glCreateShader(shaderType);
        glShaderSource(shaderID, ResourceReader.getRaw(file));
        glCompileShader(shaderID);
    
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) != GL_TRUE) {
            System.out.println(glGetShaderInfoLog(shaderID));
            String shaderTypeName = "unknown";
            switch (shaderType) {
                case GL_VERTEX_SHADER -> shaderTypeName = "vertex";
                case GL_FRAGMENT_SHADER -> shaderTypeName = "fragment";
            }
            System.err.printf("Failed to compile %s shader\n", shaderTypeName);
            System.exit(-1);
        }
        
        return shaderID;
    }
    
    public void disposeShaders() {
        glDeleteShader(vertID);
        glDeleteShader(fragID);
    }
    
    public void dispose() {
        stop();
        glDetachShader(programID, vertID);
        glDetachShader(programID, fragID);
        glDeleteProgram(programID);
    }

    public int getProgramID() {
        return programID;
    }

    public int getUniformLocation(CharSequence name) {
        Integer location = uniforms.get(name);
        if (location == null) {
            CharSequence message = !unfoundUniforms.contains(name) ? "registered" : "found or is never used";
            System.err.printf("Uniform of name \"%s\" was not %s in the shader\n", name, message);
            return -1;
        }

        return location;
    }

}

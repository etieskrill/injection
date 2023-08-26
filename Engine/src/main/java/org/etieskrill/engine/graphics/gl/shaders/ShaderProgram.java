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
    
    private static final boolean AUTO_START_ON_VARIABLE_SET = true;
    
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
    
    private void disposeShaders() {
        glDeleteShader(vertID);
        glDeleteShader(fragID);
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
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform1i(getUniformLocation(name), val);
    }
    
    @Deprecated
    public void setUniformInt_(CharSequence name, int val) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform1i(glGetUniformLocation(programID, name), val);
    }
    
    public void setUniformFloat(CharSequence name, float val) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform1f(getUniformLocation(name), val);
    }
    
    @Deprecated
    public void setUniformFloat_(CharSequence name, float val) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform1f(glGetUniformLocation(programID, name), val);
    }
    
    public void setUniformVec3(CharSequence name, Vec3 vec) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform3fv(getUniformLocation(name), vec.toDfb_());
    }
    
    @Deprecated
    public void setUniformVec3_(CharSequence name, Vec3 vec) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform3fv(glGetUniformLocation(programID, name), vec.toDfb_());
    }
    
    public void setUniformVec4(CharSequence name, Vec4 vec) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform4fv(getUniformLocation(name), vec.toDfb_());
    }
    
    public void setUniformMat3(CharSequence name, Mat3 mat) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        setUniformMat3(name, mat, false);
    }
    
    public void setUniformMat3(CharSequence name, Mat3 mat, boolean transpose) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniformMatrix3fv(getUniformLocation(name), transpose, mat.toFa_());
    }
    
    public void setUniformMat4(CharSequence name, Mat4 mat) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        setUniformMat4(name, mat, false);
    }
    
    public void setUniformMat4(CharSequence name, Mat4 mat, boolean transpose) {
        if (AUTO_START_ON_VARIABLE_SET) start();
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
    
    public void dispose() {
        stop();
        glDetachShader(programID, vertID);
        glDetachShader(programID, fragID);
        glDeleteProgram(programID);
    }
    
    protected int getUniformLocation(CharSequence name) {
        Integer location = uniforms.get(name);
        if (location == null) {
            CharSequence message = !unfoundUniforms.contains(name) ? "registered" : "found or is never used";
            //System.err.printf("[%s] Uniform of name \"%s\" was not %s in the shader\n",
            //        getClass().getSimpleName(), name, message);
            return -1;
        }

        return location;
    }

}

package org.etieskrill.engine.graphics.gl.shaders;

import glm_.mat3x3.Mat3;
import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.util.ResourceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.opengl.GL33C.*;

public abstract class ShaderProgram implements Disposable {
    
    public static boolean AUTO_START_ON_VARIABLE_SET = true;
    public static boolean CLEAR_ERROR_BEFORE_SHADER_CREATION = true;
    
    //TODO move placeholder shader here
    private static final String DEFAULT_VERTEX_FILE = "shaders/Phong.vert";
    private static final String DEFAULT_FRAGMENT_FILE = "shaders/Phong.frag";
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private int programID, vertID, fragID;
    private final Map<CharSequence, Integer> uniforms;
    private final List<CharSequence> unfoundUniforms;
    
    protected ShaderProgram() {
        String[] shaderFiles = getShaderFileNames();
        String
                vertexFile = "shaders/" + shaderFiles[0],
                fragmentFile = "shaders/" + shaderFiles[1];
        //TODO create spec for other shader types and shader programs split across multiple files
        // consider just creating a standard, such that only a single unique identifier must be passed
        
        logger.debug("Creating shader from files: {}", Arrays.toString(shaderFiles));
        
        try {
            if (CLEAR_ERROR_BEFORE_SHADER_CREATION) clearGlErrorStatus();
            createProgram(vertexFile, fragmentFile);
        } catch (IllegalStateException e) {
            logger.warn("Exception during shader creation, using default shader", e);
            createProgram(DEFAULT_VERTEX_FILE, DEFAULT_FRAGMENT_FILE);
        }
    
        this.uniforms = new HashMap<>();
        this.unfoundUniforms = new ArrayList<>();
        getUniformLocations();
    }
    
    private void createProgram(String vertFile, String fragFile) {
        programID = glCreateProgram();
    
        vertID = loadShader(vertFile, GL_VERTEX_SHADER);
        glAttachShader(programID, vertID);
        
        fragID = loadShader(fragFile, GL_FRAGMENT_SHADER);
        glAttachShader(programID, fragID);
    
        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) != GL_TRUE)
            throw new IllegalStateException("Shader program could not be linked:\n%s"
                    .formatted(glGetProgramInfoLog(programID)));
    
        disposeShaders();
    
        glValidateProgram(programID);
        if (glGetProgrami(programID, GL_VALIDATE_STATUS) != GL_TRUE)
            throw new IllegalStateException("Shader program was not successfully validated\n%s"
                    .formatted(glGetProgramInfoLog(programID)));
    
        int errorCode;
        if ((errorCode = glGetError()) != GL_NO_ERROR)
            throw new IllegalStateException("Error while creating shader: %d".formatted(errorCode));
    }
    
    private void disposeShaders() {
        glDeleteShader(vertID);
        glDeleteShader(fragID);
    }
    
    protected abstract String[] getShaderFileNames();
    
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
            logger.debug("Could not find location of uniform with name \"{}\"", name);
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
        glUniform3fv(getUniformLocation(name), vec.toFloatArray());
    }
    
    @Deprecated
    public void setUniformVec3_(CharSequence name, Vec3 vec) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform3fv(glGetUniformLocation(programID, name), vec.toFloatArray());
    }
    
    public void setUniformVec4(CharSequence name, Vec4 vec) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniform4fv(getUniformLocation(name), vec.toFloatArray());
    }
    
    public void setUniformMat3(CharSequence name, Mat3 mat) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        setUniformMat3(name, mat, false);
    }
    
    public void setUniformMat3(CharSequence name, Mat3 mat, boolean transpose) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniformMatrix3fv(getUniformLocation(name), transpose, mat.toFloatArray());
    }
    
    public void setUniformMat4(CharSequence name, Mat4 mat) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        setUniformMat4(name, mat, false);
    }
    
    public void setUniformMat4(CharSequence name, Mat4 mat, boolean transpose) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        glUniformMatrix4fv(getUniformLocation(name), transpose, mat.toFloatArray());
    }
    
    private int loadShader(String file, int shaderType) {
        String shaderTypeName = switch (shaderType) {
            case GL_VERTEX_SHADER -> "vertex";
            case GL_FRAGMENT_SHADER -> "fragment";
            default -> "unknown";
        };
        
        logger.trace("Loading {} shader from file: {}", shaderTypeName, file);
        int shaderID = glCreateShader(shaderType);
        glShaderSource(shaderID, ResourceReader.getRaw(file));
        glCompileShader(shaderID);
    
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) != GL_TRUE) {
            throw new IllegalStateException(
                    "Failed to compile %s shader from %s:\n%s"
                    .formatted(shaderTypeName, file, glGetShaderInfoLog(shaderID)));
        }
        
        return shaderID;
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
    
    private void clearGlErrorStatus() {
        glGetError();
    }
    
    @Override
    public void dispose() {
        stop();
        glDetachShader(programID, vertID);
        glDetachShader(programID, fragID);
        glDeleteProgram(programID);
    }

}

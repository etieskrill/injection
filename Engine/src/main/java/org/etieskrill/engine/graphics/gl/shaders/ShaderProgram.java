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

import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.ShaderType.*;
import static org.lwjgl.opengl.GL33C.*;

public abstract class ShaderProgram implements Disposable {
    
    public static boolean AUTO_START_ON_VARIABLE_SET = true;
    public static boolean CLEAR_ERROR_BEFORE_SHADER_CREATION = true;
    
    //TODO move placeholder shader here
    private static final String DEFAULT_VERTEX_FILE = "shaders/Phong.vert";
    private static final String DEFAULT_FRAGMENT_FILE = "shaders/Phong.frag";
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private boolean STRICT_UNIFORM_DETECTION = true;
    
    private int programID, vertID, fragID;
    private final Map<Uniform, Integer> uniforms;
    
    public enum ShaderType {
        VERTEX,
        FRAGMENT,
    }
    
    private static class ShaderFile {
        private final String file;
        private final ShaderType type;
        
        public ShaderFile(String file, ShaderType type) {
            this.file = file;
            this.type = type;
        }
        
        public String getFile() {
            return file;
        }
        
        public ShaderType getType() {
            return type;
        }
    
        @Override
        public String toString() {
            return "[%s, %s]".formatted(file, type.name().toLowerCase());
        }
    }
    
    protected ShaderProgram() {
        String[] shaderFiles = getShaderFileNames();
        String
                vertexFile = "shaders/" + shaderFiles[0],
                fragmentFile = "shaders/" + shaderFiles[1];
        ShaderFile[] files = {
                new ShaderFile(shaderFiles[0], VERTEX),
                new ShaderFile(shaderFiles[1], FRAGMENT)
        };
        //TODO create spec for other shader types and shader programs split across multiple files
        // consider just creating a standard, such that only a single unique identifier must be passed
        
        logger.debug("Creating shader from files: {}", Arrays.toString(files));
    
        this.uniforms = new HashMap<>();
        
        try {
            if (CLEAR_ERROR_BEFORE_SHADER_CREATION) clearGlErrorStatus();
            init();
            createProgram(vertexFile, fragmentFile);
            getUniformLocations();
        } catch (IllegalStateException e) {
            logger.warn("Exception during shader creation, using default shader", e);
            createProgram(DEFAULT_VERTEX_FILE, DEFAULT_FRAGMENT_FILE);
        }
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
    
    private void disposeShaders() {
        glDeleteShader(vertID);
        glDeleteShader(fragID);
    }
    
    public void start() {
        glUseProgram(programID);
    }
    
    public void stop() {
        glUseProgram(0);
    }
    
    //TODO this is so-called bullshit spaghetti code. fix it.
    public boolean setUniform(CharSequence name, Object value) {
        if (name == null) throw new NullPointerException("Name must not be null");
        if (value == null) throw new NullPointerException("Value must not be null");
    
        Uniform uniform;
        if (STRICT_UNIFORM_DETECTION) {
            Optional<Uniform> optUniform = uniforms.keySet().stream()
                    .filter(element -> name.equals(element.getName()))
                    .findAny();
            if (optUniform.isEmpty()) return false;
            uniform = optUniform.get();
    
            if (!value.getClass().equals(uniform.getType().get())) return false;
        } else {
            Uniform.Type type = Uniform.Type.getFromClass(value.getClass());
            if (type == null) return false;
            uniform = new Uniform((String) name, type);
        }
        
        return setUniform(uniform, value);
    }
    
    private boolean setUniform(Uniform uniform, Object value) {
        //This is not a boxed Integer with a null check, because in strict mode, non-registered names are
        //already filtered out in the above statement, which intellisense somehow knows apparently
        int location = STRICT_UNIFORM_DETECTION ?
                uniforms.get(uniform) :
                glGetUniformLocation(programID, uniform.getName());
        if (location == -1) return false;
    
        if (AUTO_START_ON_VARIABLE_SET) start();
        switch (uniform.type) {
            case INT, SAMPLER2D -> glUniform1i(location, (Integer) value);
            case FLOAT -> glUniform1f(location, (Float) value);
            case VEC3 -> glUniform3fv(location, ((Vec3) value).toFloatArray());
            case VEC4 -> glUniform4fv(location, ((Vec4) value).toFloatArray());
            case MAT3 -> glUniformMatrix3fv(location, false, ((Mat3) value).toFloatArray());
            case MAT4 -> glUniformMatrix4fv(location, false, ((Mat4) value).toFloatArray());
        }
    
        return true;
    }
    
    protected static class Uniform {
        protected enum Type {
            INT(Integer.class),
            FLOAT(Float.class),
            VEC3(Vec3.class),
            VEC4(Vec4.class),
            MAT3(Mat3.class),
            MAT4(Mat4.class),
            
            SAMPLER2D(Integer.class);
            
            private final Class<?> type;
    
            public static Type getFromClass(Class<?> type) {
                return Arrays.stream(Type.values())
                        .filter(value -> value.get().equals(type))
                        .findAny()
                    .orElse(null);
            }
            
            Type(Class<?> type) {
                this.type = type;
            }
    
            public Class<?> get() {
                return type;
            }
        }
        
        private final String name;
        private final Type type;
    
        public Uniform(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    
        public String getName() {
            return name;
        }
    
        public Type getType() {
            return type;
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
        
            Uniform uniform = (Uniform) o;
        
            if (!name.equals(uniform.name)) return false;
            return type.equals(uniform.type);
        }
    
        @Override
        public String toString() {
            return "%s %s".formatted(type.name().toLowerCase(), name);
        }
    }
    
    protected abstract void init();
    
    protected abstract String[] getShaderFileNames();
    
    protected abstract void getUniformLocations();
    
    protected void addUniform(String name, Uniform.Type type) {
        addUniform(new Uniform(name, type));
    }
    
    //TODO set identity values as standard, if enum-option is implemented
    protected void addUniform(Uniform uniform) {
        if (uniforms.containsKey(uniform)) return;
        int uniformLocation = glGetUniformLocation(programID, uniform.getName());
        if (uniformLocation != -1) {
            uniforms.put(uniform, uniformLocation);
            setStandardValue(uniform, uniformLocation);
            return;
        }
    
        if (STRICT_UNIFORM_DETECTION)
            throw new IllegalStateException(
                    "Cannot register non-existent uniform \"%s\" in strict mode".formatted(uniform.getName()));
        
        logger.debug("Could not find location of uniform {}", uniform);
    }
    
    private void setStandardValue(Uniform uniform, int location) {
        switch (uniform.type) {
            case INT, SAMPLER2D -> glUniform1i(location, 0);
            case FLOAT -> glUniform1f(location, 0f);
            case VEC3 -> glUniform3fv(location, new Vec3(0f).toFloatArray());
            case VEC4 -> glUniform4fv(location, new Vec4(0f).toFloatArray());
            case MAT3 -> glUniformMatrix3fv(location, false, new Mat3().identity().toFloatArray());
            case MAT4 -> glUniformMatrix4fv(location, false, new Mat4().identity().toFloatArray());
        }
    }
    
    protected void disableStrictUniformChecking() {
        this.STRICT_UNIFORM_DETECTION = false;
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

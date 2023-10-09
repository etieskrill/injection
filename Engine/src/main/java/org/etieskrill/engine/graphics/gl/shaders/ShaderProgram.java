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
import java.util.function.Function;

import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.ShaderType.FRAGMENT;
import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.ShaderType.VERTEX;
import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.Uniform.NESTED_UNIFORM_LOCATION;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL46C.*;

public abstract class ShaderProgram implements Disposable {
    
    public static boolean AUTO_START_ON_VARIABLE_SET = true;
    public static boolean CLEAR_ERROR_BEFORE_SHADER_CREATION = true;
    
    //TODO move placeholder shader here
    private static final String DEFAULT_VERTEX_FILE = "shaders/Phong.vert";
    private static final String DEFAULT_FRAGMENT_FILE = "shaders/Phong.frag";
    
    private boolean STRICT_UNIFORM_DETECTION = true;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected int programID;
    private int vertID, fragID;
    private final Map<Uniform, Integer> uniforms;
    private final Map<ArrayUniform, List<Integer>> arrayUniforms;
    
    private final boolean placeholder;
    
    public enum ShaderType {
        VERTEX,
        FRAGMENT,
        LIBRARY
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
    
    //TODO replace this by implementing main constructor with files argument
    protected ShaderProgram(boolean mock) {
        this.uniforms = null;
        this.arrayUniforms = null;
        this.placeholder = false;
    }

//    protected ShaderProgram(String vertexSource, String fragmentSource, String[] libs) {
//    }
    
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
        this.arrayUniforms = new HashMap<>();
        
        boolean placeholder = false;
        try {
            createShader(vertexFile, fragmentFile);
        } catch (ShaderException e) {
            logger.warn("Exception during shader creation, using default shader", e);
            boolean prevStrictState = STRICT_UNIFORM_DETECTION;
            STRICT_UNIFORM_DETECTION = false; //TODO quick and dirty solution bcs faulty shader's uniforms are still added
            createShader(DEFAULT_VERTEX_FILE, DEFAULT_FRAGMENT_FILE);
            STRICT_UNIFORM_DETECTION = prevStrictState;
            placeholder = true;
        }
        
        this.placeholder = placeholder;
        
        int ret = glGetError();
        if (ret != GL_NO_ERROR) logger.debug("OpenGL error during shader creation: 0x" + Integer.toHexString(ret));
        else logger.debug("Successfully created shader");
    }
    
    private void createShader(String vertFile, String fragFile) {
        if (CLEAR_ERROR_BEFORE_SHADER_CREATION) clearGlErrorStatus();
        createProgram(vertFile, fragFile);
        init();
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
            throw new ShaderCreationException("Shader porgram could not be linked", glGetProgramInfoLog(programID));
    
        disposeShaders();

        //TODO write test engine to validate shaders and such pre-launch/via a separate script (unit-test-esque)
        //this actually validates based on the current OpenGL state, meaning that here, a completely uninitialised
        //program is being tested, which will, in the majority of cases, fail.
//        glValidateProgram(programID);
//        if (glGetProgrami(programID, GL_VALIDATE_STATUS) != GL_TRUE)
//            throw new IllegalStateException("Shader program was not successfully validated\n%s"
//                    .formatted(glGetProgramInfoLog(programID)));
    
        int errorCode;
        if ((errorCode = glGetError()) != GL_NO_ERROR)
            throw new ShaderCreationException("Error while creating shader", errorCode);
    }
    
    //TODO add loader for shader objects and wrap calls to this method in said loader
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
            throw new ShaderCreationException("Failed to compile %s shader from %s"
                    .formatted(shaderTypeName, file), glGetShaderInfoLog(shaderID));
        }
        
        return shaderID;
    }

    //TODO use named string arbs to modularise shaders
    private void loadLibrary(String file) {
        glNamedStringARB(GL_SHADER_INCLUDE_ARB, file.split("\\.")[0], ResourceReader.getRaw(file));
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

    private final HashMap<String, Boolean> missingUniforms = new HashMap<>();
    
    public void setUniform(String name, Object value) {
        setUniform(name, value, true);
    }
    
    //TODO this is so-called bullshit spaghetti code. fix it.
    public void setUniform(String name, Object value, boolean strict) {
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(value, "Value must not be null");
        
        Uniform uniform = getUniform(uniforms, name, value, strict, type -> new Uniform(name, type));
        if (uniform != null && uniform.getType() == Uniform.Type.STRUCT) {
            if (value instanceof UniformMappable mappable)
                mappable.map(MapperShaderProgram.get(this, name, -1));
            else
                throw new ShaderUniformException("Struct uniform must implement UniformMappable interface");
            return;
        }
        if (uniform == null) {
            if (STRICT_UNIFORM_DETECTION & strict)
                throw new ShaderUniformException("Attempted to set unregistered uniform: " + name);
            if (missingUniforms.get(name) == null) logger.trace("Attempted to set unregistered uniform: " + name);
            missingUniforms.put(name, true);
            return;
        }
        
        int location = STRICT_UNIFORM_DETECTION & strict ?
                uniforms.get(uniform) :
                glGetUniformLocation(programID, uniform.getName());
        if (location == -1) {
            if (missingUniforms.get(name) == null) logger.trace("Attempted to set nonexistent uniform: " + name);
            missingUniforms.put(name, true);
            return;
        }
        setUniformValue(uniform, location, value);
    }
    
//    public boolean setUniformArray(String name, Object[] value) {
//    }
    
    public void setUniformArray(String name, int index, Object value) {
        if (name == null) throw new NullPointerException("Name must not be null");
        if (value == null) throw new NullPointerException("Value must not be null");
        
        ArrayUniform uniform = getUniform(arrayUniforms, name, value, true, type -> new ArrayUniform(name, index, type));
        if (uniform == null) {
            if (missingUniforms.get(name) == null) logger.trace("Attempted to set unregistered uniform: " + name);
            missingUniforms.put(name, true);
            return;
        }
        
        if (index < 0 || index > uniform.getSize()) return;
    
        if (uniform.getType() == Uniform.Type.STRUCT) {
            if (value instanceof UniformMappable mappable)
                mappable.map(MapperShaderProgram.get(this, name, index));
            else
                throw new ShaderUniformException("Struct uniform must implement UniformMappable interface");
            return;
        }
        
        setUniform(uniform.getNameWith(index), value, false);
    }
    
    //TODO simplify, either here or by implementing directly in setUniform/setUniformArray methods
    private <T extends Uniform> T getUniform(Map<T, ?> map, String name, Object value, boolean strict, Function<Uniform.Type, T> supplier) {
        T uniform;
        if (STRICT_UNIFORM_DETECTION & strict) {
            Optional<T> optUniform = map.keySet().stream()
                    .filter(element -> name.equals(element.getName()))
                    .findAny();
            if (optUniform.isEmpty()) return null;
            uniform = optUniform.get();
            if (uniform.getType() == Uniform.Type.STRUCT) return uniform;
            if (!value.getClass().equals(uniform.getType().get())) return null;
        } else {
            Uniform.Type type = Uniform.Type.getFromValue(value);
            if (type == null) return null;
            uniform = supplier.apply(type);
        }
        return uniform;
    }
    
    void setUniformValue(Uniform uniform, int location, Object value) {
        if (AUTO_START_ON_VARIABLE_SET) start();
        switch (uniform.getType()) {
            case INT, SAMPLER2D -> glUniform1i(location, (Integer) value);
            case FLOAT -> glUniform1f(location, (Float) value);
            case BOOLEAN -> glUniform1f(location, (boolean) value ? 1 : 0);
            case VEC3 -> glUniform3fv(location, ((Vec3) value).toFloatArray());
            case VEC4 -> glUniform4fv(location, ((Vec4) value).toFloatArray());
            case MAT3 -> glUniformMatrix3fv(location, false, ((Mat3) value).toFloatArray());
            case MAT4 -> glUniformMatrix4fv(location, false, ((Mat4) value).toFloatArray());
        }
    }
    
    //TODO if multi-threaded: add thread lock
    public static abstract class MapperShaderProgram extends ShaderProgram {
        private static final MapperShaderProgram instance = new MapperShaderProgram() {
            @Override protected void init() {}
            @Override protected String[] getShaderFileNames() { return new String[0]; }
            @Override protected void getUniformLocations() {}
        };
        
        private String structName;
        private int arrayIndex;
        
        static MapperShaderProgram get(ShaderProgram shader, String structName, int arrayIndex) {
            instance.programID = shader.programID;
            instance.structName = structName;
            instance.arrayIndex = arrayIndex;
            return instance;
        }
        
        private MapperShaderProgram() {
            super(true);
        }
    
        public MapperShaderProgram setUniformStructValue(String varName, Object value) {
            //TODO strict nested uniforms: probably hook into here for registration
            Uniform.Type type = Uniform.Type.getFromValue(value);
            if (type == null) return this;
            Uniform uniform;
            if (arrayIndex <= -1)
                uniform = new Uniform(structName + "." + varName, type);
            else
                uniform = new ArrayUniform(structName + "." + varName, arrayIndex, type);
            String name = arrayIndex <= -1 ? uniform.getName() : ((ArrayUniform) uniform).getNameWith(arrayIndex);
            int location = glGetUniformLocation(programID, name);
            if (location == -1) return this;
            setUniformValue(uniform, location, value);
            return this;
        }
    }
    
    //TODO move to separate class and replace constructors with factory methods
    protected static class Uniform {
        protected static final int INVALID_UNIFORM_LOCATION = -1;
        protected static final int NESTED_UNIFORM_LOCATION = -2;
        
        private final String name;
        private final Type type;
//        private final boolean wildcard;
        
        protected enum Type {
            INT(Integer.class),
            FLOAT(Float.class),
            BOOLEAN(Boolean.class),
            VEC3(Vec3.class),
            VEC4(Vec4.class),
            MAT3(Mat3.class),
            MAT4(Mat4.class),
            
            SAMPLER2D(Integer.class),
            SAMPLER_CUBE_MAP(Integer.class),
            
            STRUCT(UniformMappable.class);
            
            private final Class<?> type;
            
            public static Type getFromValue(Object value) {
                if (value instanceof UniformMappable) return STRUCT;
                return Arrays.stream(Type.values())
                        .filter(type -> type.get().equals(value.getClass()))
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
    
        public Uniform(String name, Type type) {
            //TODO array/wildcard detection
//            if (name.contains("*")) {
//                if (numArrayIndices > 0 || name.indexOf("*") != name.length() - 1) throw new IllegalArgumentException(
//                        "Name may only contain either an array index or a wildcard, " +
//                        "and the wildcard must be at the end of the name: " + name);
//                wildcard = true;
//            } else wildcard = false;
            
            this.name = name;
            this.type = type;
        }
        
//        public boolean hasWildcard() {
//            return wildcard;
//        }
    
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
    
    protected static class ArrayUniform extends Uniform {
        private final int size;
        
        private int arrayIndex;
        
        public ArrayUniform(String name, int size, Type type) {
            super(name, type);
            
            if (name.contains("*")) throw new ShaderUniformException(
                    "Uniform of array type may not contain any wildcard specifiers", name);
    
            int numArrayIndices = 0;
            for (int i = 0; i < name.length(); i++) {
                if (name.charAt(i) == '$') {
                    arrayIndex = i;
                    numArrayIndices++;
                }
                if (numArrayIndices > 1) throw new ShaderUniformException(
                        "Only a single array index placeholder may be specified", name);
            }
    
            this.size = size;
        }
        
        public String getNameWith(int index) {
            return getName().replace("$", Integer.toString(index));
        }
    
        public int getArrayIndex() {
            return arrayIndex;
        }
    
        public int getSize() {
            return size;
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
        
            ArrayUniform that = (ArrayUniform) o;
        
            // Size is irrelevant for equality
            return arrayIndex == that.arrayIndex;
        }
    }
    
    protected abstract void init();
    
    protected abstract String[] getShaderFileNames();
    
    protected abstract void getUniformLocations();
    
    protected void addUniform(String name, Uniform.Type type) {
        Uniform uniform = new Uniform(name, type);
        if (uniforms.containsKey(uniform)) return;
        
        if (type == Uniform.Type.STRUCT) {
            uniforms.put(uniform, NESTED_UNIFORM_LOCATION); //TODO remove filler value if possible, since type is present a/w
            logger.trace("Registered nested uniform {}", uniform.getName());
            return;
        }
        
        int uniformLocation = glGetUniformLocation(programID, uniform.getName());
        if (uniformLocation != -1) {
            uniforms.put(uniform, uniformLocation);
            setStandardValue(uniform, uniformLocation);
            logger.trace("Registered uniform {}", uniform.getName());
            return;
        }
    
        if (STRICT_UNIFORM_DETECTION)
            throw new ShaderUniformException(
                    "Cannot register non-existent or non-used uniform in strict mode", uniform.getName());
        
        logger.debug("Could not find location of uniform {}", uniform);
    }
    
    protected void addUniformArray(String name, int size, Uniform.Type type) {
        List<Integer> locations = new ArrayList<>();
        ArrayUniform uniform = new ArrayUniform(name, size, type);
        
        if (arrayUniforms.containsKey(uniform)) return;
        
        if (type == Uniform.Type.STRUCT) {
            //TODO check for values
            arrayUniforms.put(uniform, null);
            return;
        }
        
        for (int i = 0; i < size; i++) {
            int location = glGetUniformLocation(programID, uniform.getNameWith(i));
            if (location != -1) {
                locations.add(location);
                setStandardValue(uniform, location);
                logger.trace("Registered uniform {}", uniform.getName());
                continue;
            }
            
            if (STRICT_UNIFORM_DETECTION)
                throw new ShaderUniformException(
                        "Cannot register non-existent or unused uniform in strict mode", uniform.getName());
            
            logger.debug("Could not find location of uniform {}", uniform);
        }
        
        arrayUniforms.put(uniform, locations);
    }
    
    private void setStandardValue(Uniform uniform, int location) {
        switch (uniform.type) {
            case INT, BOOLEAN, SAMPLER2D -> glUniform1i(location, 0);
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
    
    public boolean isPlaceholder() {
        return placeholder;
    }
    
}

package org.etieskrill.engine.graphics.gl.shaders;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.util.ResourceReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.etieskrill.engine.config.ResourcePaths.SHADER_PATH;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkError;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.ShaderType.*;
import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.Uniform.INVALID_UNIFORM_LOCATION;
import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.Uniform.NESTED_UNIFORM_LOCATION;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL46C.*;

public abstract class ShaderProgram implements Disposable {

    public static boolean AUTO_START_ON_VARIABLE_SET = true;
    public static boolean CLEAR_ERROR_BEFORE_SHADER_CREATION = true;

    //TODO move placeholder shader here
    private static final String DEFAULT_VERTEX_FILE = SHADER_PATH + "Phong.vert";
    private static final String DEFAULT_FRAGMENT_FILE = SHADER_PATH + "Phong.frag";

    private boolean STRICT_UNIFORM_DETECTION = true;

    protected int programID;
    private int vertID, geomID = -1, fragID;
    private final Map<String, Uniform> uniforms;
    private final Map<String, ArrayUniform> arrayUniforms;

    private final boolean placeholder;

    private final Map<String, Integer> nonstrictUniformCache = new HashMap<>();
    private final Set<String> missingUniforms = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public enum ShaderType {
        VERTEX,
        GEOMETRY,
        FRAGMENT,
        //LIBRARY
    }

    protected record ShaderFile(
            String name,
            ShaderType type
    ) {
        public String getName() {
            return name;
        }

        public ShaderType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShaderFile that = (ShaderFile) o;
            return Objects.equals(name, that.name) && type == that.type;
        }

        @Override
        public String toString() {
            return "[%s, %s]".formatted(name, type.name().toLowerCase());
        }
    }

    //TODO replace this by implementing main constructor with files argument
    protected ShaderProgram(boolean mock) {
        this.uniforms = null;
        this.arrayUniforms = null;
        this.placeholder = false;
    }

    protected ShaderProgram() {
        Set<ShaderFile> files = Arrays.stream(getShaderFileNames()).map(file -> {
            String[] parts = file.split("\\.");
            ShaderType type = switch (parts[parts.length - 1]) {
                case "vert" -> VERTEX;
                case "geom" -> GEOMETRY;
                case "frag" -> FRAGMENT;
                default -> throw new ShaderCreationException("Cannot load shader with unknown file extension: " + file);
            };
            return new ShaderFile(file, type);
        }).collect(Collectors.toSet());

        if (!files.stream().map(ShaderFile::getType).collect(Collectors.toSet()).containsAll(Set.of(VERTEX, FRAGMENT)))
            throw new ShaderCreationException("Shader must have one vertex and one fragment shader");
        //TODO create spec for other shader types and shader programs split across multiple files
        // consider just creating a standard, such that only a single unique identifier must be passed

        logger.debug("Creating shader from files: {}", files);

        this.uniforms = new HashMap<>();
        this.arrayUniforms = new HashMap<>();

        boolean placeholder = false;
        try {
            createShader(files);
        } catch (ShaderException e) {
            logger.warn("Exception during shader creation, using default shader", e);
            boolean prevStrictState = STRICT_UNIFORM_DETECTION;
            STRICT_UNIFORM_DETECTION = false; //TODO quick and dirty solution bcs faulty shader's uniforms are still added
            createShader(Set.of(
                    new ShaderFile(DEFAULT_VERTEX_FILE, VERTEX),
                    new ShaderFile(DEFAULT_FRAGMENT_FILE, FRAGMENT)
            ));
            STRICT_UNIFORM_DETECTION = prevStrictState;
            placeholder = true;
        }

        this.placeholder = placeholder;

        if (checkError("OpenGL error during shader creation"))
            logger.debug("Successfully created shader");
    }

    private void createShader(Set<ShaderFile> files) {
        if (CLEAR_ERROR_BEFORE_SHADER_CREATION) clearError();
        createProgram(files);
        init();

        start();
        getUniformLocations();
        stop();
    }

    private void createProgram(Set<ShaderFile> files) {
        programID = glCreateProgram();

        vertID = loadShader(files, VERTEX);
        glAttachShader(programID, vertID);

        if (files.stream().map(ShaderFile::getType).anyMatch(type -> type == GEOMETRY)) {
            geomID = loadShader(files, GEOMETRY);
            glAttachShader(programID, geomID);
        }

        fragID = loadShader(files, FRAGMENT);
        glAttachShader(programID, fragID);

        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) != GL_TRUE)
            throw new ShaderCreationException("Shader program could not be linked", glGetProgramInfoLog(programID));

        disposeShaders();

        //TODO write test engine to validate shaders and such pre-launch/via a separate script (unit-test-esque)
        //this actually validates based on the current OpenGL state, meaning that here, a completely uninitialised
        //program is being tested, which will, in the majority of cases, fail.
//        glValidateProgram(programID);
//        if (glGetProgrami(programID, GL_VALIDATE_STATUS) != GL_TRUE)
//            throw new IllegalStateException("Shader program was not successfully validated\n%s"
//                    .formatted(glGetProgramInfoLog(programID)));

        checkError("Error while creating shader");
    }

    //TODO add loader for shader objects and wrap calls to this method in said loader
    private int loadShader(Set<ShaderFile> files, ShaderType type) {
        ShaderFile file = files.stream()
                .filter(shaderFile -> shaderFile.getType() == type)
                .findAny()
                .orElseThrow(() -> new ShaderCreationException("No " + type.name().toLowerCase() + " shader file was found"));

        logger.trace("Loading {} shader from file: {}", file.getType().name().toLowerCase(), file);

        int shaderID = glCreateShader(switch (file.getType()) {
            case VERTEX -> GL_VERTEX_SHADER;
            case GEOMETRY -> GL_GEOMETRY_SHADER;
            case FRAGMENT -> GL_FRAGMENT_SHADER;
        });

        glShaderSource(shaderID, getShaderSource(file));
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) != GL_TRUE)
            throw new ShaderCreationException("Failed to compile %s shader from %s"
                    .formatted(file.getType().name().toLowerCase(), file), glGetShaderInfoLog(shaderID));

        return shaderID;
    }

    private String getShaderSource(ShaderFile file) {
        String qualifiedName = SHADER_PATH + file.getName();
        return ResourceReader.getClasspathResource(qualifiedName);
    }

    //TODO use named string arbs to modularise shaders
    private void loadLibrary(String file) {
        glNamedStringARB(GL_SHADER_INCLUDE_ARB, file.split("\\.")[0], ResourceReader.getClasspathResource(file));
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

    public void setUniform(@NotNull String name, @NotNull Object value) {
        setUniform(name, value, uniforms, true, false);
    }

    public void setUniform(@NotNull String name, @NotNull Object value, boolean strict) {
        setUniform(name, value, uniforms, strict, false);
    }

    public void setUniformArray(@NotNull String name, @NotNull Object[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException("Uniform values must not be empty");

        setUniform(name, values, arrayUniforms, true, true);
    }

    public void setUniformArray(@NotNull String name, int index, @NotNull Object value) {
        setUniform(name + "[" + index + "]", value, false);
    }

    private <T extends Uniform> void setUniform(String name, Object value, Map<String, T> uniformMap, boolean strict, boolean array) {
        if (name.isBlank()) throw new IllegalArgumentException("Name must not be empty");

        T uniform = uniformMap.get(name);
        if (uniform != null) {
            setRegisteredUniform(uniform, value, array);
            return;
        }

        if (STRICT_UNIFORM_DETECTION && strict)
            throw new ShaderUniformException("Attempted to set unregistered uniform in strict mode", name);

        if (!missingUniforms.contains(name)) {
            logger.warn("Setting unregistered uniform '{}'", name);
            missingUniforms.add(name);
        }

        setUnregisteredUniform(name, value, array);
    }

    private void setRegisteredUniform(Uniform uniform, Object value, boolean array) {
        if (uniform.getType() == Uniform.Type.STRUCT) {
            if (array && ((Object[]) value)[0] instanceof UniformMappable) {
                UniformMappable[] mappables = (UniformMappable[]) value;
                for (int i = 0; i < mappables.length; i++) {
                    mappables[i].map(UniformMapper.get(this, uniform.getName() + "[" + i + "]"));
                }

//                throw new UnsupportedOperationException("Mapping struct arrays is currently not supported");
            } else if (!array && value instanceof UniformMappable mappable) {
                mappable.map(UniformMapper.get(this, uniform.getName()));
            } else {
                throw new ShaderUniformException("Struct uniform must implement UniformMappable interface");
            }
            return;
        }

        if (array && !uniform.getType().get().equals(((Object[]) value)[0].getClass())
                || !array && !uniform.getType().get().equals(value.getClass())) {
            throw new ShaderUniformException("Uniform " + uniform.getName() + " is present but expected type " +
                    uniform.getType().get().getSimpleName() + " does not match value type " + value.getClass().getSimpleName());
        }

        if (!array) setUniformValue(uniform.getType(), uniform.getLocation(), value);
        else setUniformArrayValue(uniform.getType(), uniform.getLocation(), (Object[]) value);
    }

    private void setUnregisteredUniform(String name, Object value, boolean array) {
        if (value instanceof UniformMappable mappable) {
            mappable.map(UniformMapper.get(this, name));
            return;
        }

        Integer location = nonstrictUniformCache.get(name);
        if (location == null) {
            location = glGetUniformLocation(programID, name);
            nonstrictUniformCache.put(name, location);
        }

        if (location == -1) {
            if (!missingUniforms.contains(name)) logger.trace("Attempted to set nonexistent uniform: " + name);
            missingUniforms.add(name);
            return;
        }

        if (!array) setUniformValue(Uniform.Type.getFromValue(value), location, value);
        else setUniformArrayValue(Uniform.Type.getFromValue(((Object[]) value)[0]), location, (Object[]) value);
    }

    void setUniformValue(Uniform.Type type, int location, Object value) {
        if (type == null)
            throw new ShaderUniformException("Could not determine uniform type for " + value.getClass().getSimpleName());

        if (AUTO_START_ON_VARIABLE_SET) start();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            switch (type) {
                case INT, SAMPLER2D, SAMPLER_CUBE_MAP -> glUniform1i(location, (Integer) value);
                case FLOAT -> glUniform1f(location, (Float) value);
                case BOOLEAN -> glUniform1f(location, (boolean) value ? 1 : 0);
                case VEC2 -> glUniform2fv(location, ((Vector2f) value).get(stack.mallocFloat(2)));
                case VEC2I -> glUniform2iv(location, ((Vector2i) value).get(stack.mallocInt(2)));
                case VEC3 -> glUniform3fv(location, ((Vector3f) value).get(stack.mallocFloat(3)));
                case VEC4 -> glUniform4fv(location, ((Vector4f) value).get(stack.mallocFloat(4)));
                case MAT3 -> glUniformMatrix3fv(location, false, ((Matrix3f) value).get(stack.mallocFloat(9)));
                case MAT4 -> glUniformMatrix4fv(location, false, ((Matrix4f) value).get(stack.mallocFloat(16)));
            }
        }
    }

    void setUniformArrayValue(Uniform.Type type, int location, Object[] value) {
        if (AUTO_START_ON_VARIABLE_SET) start();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            switch (type) {
                case INT, SAMPLER2D, SAMPLER_CUBE_MAP -> {
                    IntBuffer ints = stack.mallocInt(value.length);
                    for (Object o : value) ints.put((Integer) o);
                    glUniform1iv(location, ints.rewind());
                }
                case FLOAT -> {
                    FloatBuffer floats = stack.mallocFloat(value.length);
                    for (Object o : value) floats.put((Float) o);
                    glUniform1fv(location, floats.rewind());
                }
                case BOOLEAN -> {
                    IntBuffer ints = stack.mallocInt(value.length);
                    for (Object o : value) ints.put((Boolean) o ? 1 : 0);
                    glUniform1iv(location, ints.rewind());
                }
                case VEC2 -> {
                    FloatBuffer vector2s = stack.mallocFloat(2 * value.length);
                    for (Object o : value) ((Vector2f) o).get(vector2s).position(vector2s.position() + 2);
                    glUniform2fv(location, vector2s.rewind());
                }
                case VEC2I -> {
                    IntBuffer vector2is = stack.mallocInt(2 * value.length);
                    for (Object o : value) ((Vector2i) o).get(vector2is).position(vector2is.position() + 2);
                    glUniform2iv(location, vector2is.rewind());
                }
                case VEC3 -> {
                    FloatBuffer vector3s = stack.mallocFloat(3 * value.length);
                    for (Object o : value) ((Vector3f) o).get(vector3s).position(vector3s.position() + 3);
                    glUniform3fv(location, vector3s.rewind());
                }
                case VEC4 -> {
                    FloatBuffer vector4s = stack.mallocFloat(4 * value.length);
                    for (Object o : value) ((Vector4f) o).get(vector4s).position(vector4s.position() + 4);
                    glUniform4fv(location, vector4s.rewind());
                }
                case MAT3 -> {
                    FloatBuffer matrix3s = stack.mallocFloat(9 * value.length);
                    for (Object o : value) ((Matrix3f) o).get(matrix3s).position(matrix3s.position() + 9);
                    glUniformMatrix3fv(location, false, matrix3s.rewind());
                }
                case MAT4 -> {
                    FloatBuffer matrix4s = stack.mallocFloat(16 * value.length);
                    for (Object o : value) ((Matrix4f) o).get(matrix4s).position(matrix4s.position() + 16);
                    glUniformMatrix4fv(location, false, matrix4s.rewind());
                }
            }
        }
    }

    //TODO if multi-threaded: add thread lock
    //TODO update only if dirty -> push responsibility to UniformMappables?
    public static class UniformMapper {
        private static final UniformMapper instance = new UniformMapper();

        private ShaderProgram shader;
        private String structName;

        static UniformMapper get(ShaderProgram shader, String structName) {
            instance.shader = shader;
            instance.structName = structName;
            return instance;
        }

        private UniformMapper() {
        }

        public UniformMapper map(String varName, Object value) {
            //TODO strict nested uniforms: probably hook into here for registration
            shader.setUniform(structName + "." + varName, value, false);
            return this;
        }
    }

    //TODO move to separate class and replace constructors with factory methods
    public static class Uniform {
        protected static final int INVALID_UNIFORM_LOCATION = -1;
        protected static final int NESTED_UNIFORM_LOCATION = -2;

        private final String name;
        private final Type type;
        private final int location;

        public enum Type {
            INT(Integer.class),
            FLOAT(Float.class),
            BOOLEAN(Boolean.class),
            VEC2(Vector2f.class),
            VEC2I(Vector2i.class),
            VEC3(Vector3f.class),
            VEC4(Vector4f.class),
            MAT3(Matrix3f.class),
            MAT4(Matrix4f.class),

            SAMPLER2D(Integer.class),
            SAMPLER_CUBE_MAP(Integer.class),

            STRUCT(UniformMappable.class);

            private final Class<?> type;

            private static final Type[] values = values();

            public static @Nullable Type getFromValue(Object value) {
                if (value instanceof UniformMappable) return STRUCT;
                for (Type type : values) {
                    if (type.get().equals(value.getClass())) return type;
                }
                return null;
            }

            Type(Class<?> type) {
                this.type = type;
            }

            public Class<?> get() {
                return type;
            }
        }

        public Uniform(String name, Type type, int location) {
            this.name = name;
            this.type = type;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public int getLocation() {
            return location;
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

        public ArrayUniform(String name, int size, Type type, int location) {
            super(name, type, location);
            this.size = size;
        }

        public String getNameWith(int index) {
            if (index > size) throw new ShaderUniformException("Array index out of bounds", super.name);
            return getName().replace("$", Integer.toString(index));
        }

        public int getSize() {
            return size;
        }
    }

    protected void init() {
    }

    protected abstract String[] getShaderFileNames();

    protected abstract void getUniformLocations();

    protected void addUniform(String name, Uniform.Type type) {
        addUniform(name, type, true);
    }

    protected void addUniform(String name, Uniform.Type type, boolean defaultValues) {
        if (uniforms.containsKey(name)) return;

        if (type == Uniform.Type.STRUCT) {
            uniforms.put(name, new Uniform(name, type, NESTED_UNIFORM_LOCATION));
            logger.trace("Registered nested uniform '{}'", name);
            return;
        }

        int location = glGetUniformLocation(programID, name);
        if (location != INVALID_UNIFORM_LOCATION) {
            uniforms.put(name, new Uniform(name, type, location));
            if (defaultValues) setStandardValue(type, location);
            logger.trace("Registered uniform '{}'", name);
            return;
        }

        if (STRICT_UNIFORM_DETECTION)
            throw new ShaderUniformException(
                    "Cannot register non-existent or non-used uniform in strict mode", name);

        logger.debug("Could not find uniform '{}' of type {}", name, type);
    }

    protected void addUniformArray(String name, int size, Uniform.Type type) {
        addUniformArray(name, size, type, true);
    }

    protected void addUniformArray(String name, int size, Uniform.Type type, boolean defaultValues) {
        if (arrayUniforms.containsKey(name)) return;

        if (type == Uniform.Type.STRUCT) {
            arrayUniforms.put(name, new ArrayUniform(name, size, type, NESTED_UNIFORM_LOCATION));
            logger.trace("Registered nested array uniform '{}'", name);
            return;
        }

        int location = glGetUniformLocation(programID, name);
        if (location != INVALID_UNIFORM_LOCATION) {
            arrayUniforms.put(name, new ArrayUniform(name, size, type, location));
            if (defaultValues) setStandardValue(type, location);
            logger.trace("Registered array uniform '{}'", name);
            return;
        }

        if (STRICT_UNIFORM_DETECTION)
            throw new ShaderUniformException(
                    "Cannot register non-existent or non-used array uniform in strict mode", name);

        logger.debug("Could not find array uniform '{}' of type {}", name, type);
    }

    private void setStandardValue(Uniform.Type type, int location) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            switch (type) {
                case INT, BOOLEAN, SAMPLER2D -> glUniform1i(location, 0);
                case FLOAT -> glUniform1f(location, 0f);
                case VEC2 -> glUniform2fv(location, stack.callocFloat(2));
                case VEC2I -> glUniform2iv(location, stack.callocInt(2));
                case VEC3 -> glUniform3fv(location, stack.callocFloat(3));
                case VEC4 -> glUniform4fv(location, stack.callocFloat(4));
                case MAT3 -> glUniformMatrix3fv(location, false, new Matrix3f().identity().get(stack.callocFloat(9)));
                case MAT4 -> glUniformMatrix4fv(location, false, new Matrix4f().identity().get(stack.callocFloat(16)));
            }
        }
    }

    protected void disableStrictUniformChecking() {
        this.STRICT_UNIFORM_DETECTION = false;
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